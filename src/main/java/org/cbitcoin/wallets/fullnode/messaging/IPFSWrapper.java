package org.cbitcoin.wallets.fullnode.messaging;

import org.cbitcoin.wallets.fullnode.daemon.CBTCClientCaller;
import org.cbitcoin.wallets.fullnode.daemon.CBTCInstallationObserver;
import org.cbitcoin.wallets.fullnode.daemon.CBTCInstallationObserver.*;
import org.cbitcoin.wallets.fullnode.daemon.CommandExecutor;
import org.cbitcoin.wallets.fullnode.util.Log;
import org.cbitcoin.wallets.fullnode.util.OSUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cbitcoin.wallets.fullnode.util.OSUtil.OS_TYPE;
import org.cbitcoin.wallets.fullnode.util.Util;

public class IPFSWrapper {
    private JFrame parentFrame;

    private Process IPFSProcess;

    private final Pattern ipfsUrlPattern = Pattern.compile(
            "https?://[a-zA-Z0-9\\.\\-]+(:[0-9]{2,5})?/ipfs/[a-zA-Z0-9]{15,100}");


    public IPFSWrapper(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        this.IPFSProcess = null;
    }


    public boolean isIPFSURL(String url) {
        return ipfsUrlPattern.matcher(url).matches();
    }


    public String replaceIPFSHTMLLinks(String html) {
        Matcher m = ipfsUrlPattern.matcher(html);
        StringBuffer sb = new StringBuffer(html.length());
        while (m.find()) {
            String link = m.group(0);
            link = "<a href=\"" + link + "\">" + link + "</a>";
            m.appendReplacement(sb, Matcher.quoteReplacement(link));
        }
        m.appendTail(sb);
        return sb.toString();
    }


    public void followIPFSLink(URL u)
            throws IOException, InterruptedException, URISyntaxException {
        if (this.ensureIPFSIsRunning()) {
            Log.info("Opening IPFS link: {0}", u.toString());
            Desktop.getDesktop().browse(u.toURI());
        } else {
            Log.info("NOT opening IPFS link: {0} due to IPFS not running!!!", u.toString());
        }
    }


    // Returns null or [name](link)
    public String shareFileViaIPFS()
            throws IOException, InterruptedException {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Share File via IPFS");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int result = fileChooser.showOpenDialog(this.parentFrame);

        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File f = fileChooser.getSelectedFile();

        Cursor oldCursor = this.parentFrame.getCursor();
        try {
            this.parentFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            Log.info("Sharing file: {0}", f.getCanonicalPath());

            if (!this.ensureIPFSIsRunning()) {
                return null;
            }

            String ipfs = this.getIPFSFullExecutablePath();
            String pathParameter = CBTCClientCaller.wrapStringParameter(f.getCanonicalPath());

            CommandExecutor exec = new CommandExecutor(new String[]
                    {
                            ipfs, "add", "--quieter", pathParameter
                    });

            String strResponse = exec.execute().trim();

            Log.info("IPFS hash of added file is: " + strResponse);

            // TODO: add via HTTP to some public writable IPFS gateway
            //this.uploadIPFSDataViaPost(f, "http://localhost:8080/ipfs/");

            this.parentFrame.setCursor(oldCursor);

            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection("http://localhost:8080/ipfs/" + strResponse), null);

            JOptionPane.showMessageDialog(
                    this.parentFrame,
                    "The file " + f.getName() + " has been shared successfully via IPFS. It may be\n" +
                            "reached by other users (who have a local IPFS server running) via IPFS link: \n" +
                            "http://localhost:8080/ipfs/" + strResponse + "\n\n" +
                            "The link has been added to the messaging text box and also copied to the clipboard.\n",
                    "File shared successfully", JOptionPane.INFORMATION_MESSAGE);

            return "[" + f.getName() + "](" +
                    "http://localhost:8080/ipfs/" + strResponse + ")";
        } catch (Exception wce) {
            Log.error("Unexpected error: ", wce);

            JOptionPane.showMessageDialog(
                    this.parentFrame,
                    "An unexpected error occurred while sharing file via IPFS!" +
                            "\n" + wce.getMessage().replace(",", ",\n"),
                    "Error in importing wallet private keys...", JOptionPane.ERROR_MESSAGE);
            return null;
        } finally {
            this.parentFrame.setCursor(oldCursor);
        }
    }


    // true if started OK
    private boolean ensureIPFSIsRunning()
            throws IOException, InterruptedException {
        // TODO: As of Nov 2017 the IPFS wallet integration is suspended. This method just returns
        // true. This is to be corrected when IPFS integration resumes.
        if (true) {
            return !false;
        }

        if (!isIPFSWrapperRunning()) {
            if (!this.getUserConsentToStartIPFS()) {
                return false;
            }

            return this.startIPFS();
        }

        return true;
    }


    // true if started OK
    private boolean startIPFS()
            throws IOException, InterruptedException {
        // Warn user if executable and dir are missing!
        File dir = new File(this.getIPFSDirectory());
        if ((!dir.exists()) || (!dir.isDirectory())) {
            JOptionPane.showMessageDialog(
                    this.parentFrame,
                    "The IPFS executables are expected to be found in directory:\n" +
                            dir.getCanonicalPath() + "\n" +
                            "However this directory is missing! IPFS cannot be started!",
                    "IPFS directory is not available", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        File ipfsCmd = new File(this.getIPFSFullExecutablePath());
        if ((!ipfsCmd.exists()) || (!ipfsCmd.isFile())) {
            JOptionPane.showMessageDialog(
                    this.parentFrame,
                    "The IPFS command executable:\n" +
                            ipfsCmd.getCanonicalPath() + "\n" +
                            "needs to be available in order to start an IPFS Server on this PC." +
                            "However this executable file is missing! IPFS cannot be started!",
                    "IPFS executable is not available", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Check IPFS config and possibly initialize it
        File userhome = OSUtil.getUserHomeDirectory();
        File ipfsConfig = new File(userhome, ".ipfs" + File.separator + "config");
        if (!ipfsConfig.exists()) {
            Log.info("IPFS configuration file {0} does not exist. IPFS will be initilaized!",
                    ipfsConfig.getCanonicalPath());
            CommandExecutor initilaizer = new CommandExecutor(
                    new String[]
                            {
                                    this.getIPFSFullExecutablePath(), "init"
                            }
            );

            String initResponse = initilaizer.execute();

            Log.info("IPFS initilaization messages: {0}", initResponse);
        }

        // Finally start IPFS
        CommandExecutor starter = new CommandExecutor(
                new String[]
                        {
                                this.getIPFSFullExecutablePath(), "daemon"
                        }
        );

        this.IPFSProcess = starter.startChildProcess();

        // Wait 20 sec to make sure the daemon is started
        // TODO: better way to find out if it is started
        Cursor oldCursor = this.parentFrame.getCursor();
        this.parentFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Thread.sleep(20 * 1000);
        this.parentFrame.setCursor(oldCursor);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                Log.info("Stopping IPFS...");
                try {
                    IPFSWrapper.this.IPFSProcess.destroy();
                } catch (Exception bad) {
                    Log.error("Couldn't stop IPFS!", bad);
                }
            }
        });

        return true;
    }


    // Returns true if the user agrees
    private boolean getUserConsentToStartIPFS()
            throws IOException {
        String userDir = OSUtil.getSettingsDirectory();
        File ipfsMessageFlagFile = new File(userDir + File.separator + "ipfsInfoShown.flag");
        if (ipfsMessageFlagFile.exists()) {
            return true;
        }

        Object[] options = {"Yes", "No", "Yes and do not show this message again"};

        int option = JOptionPane.showOptionDialog(
                this.parentFrame,
                "This operation will start an IPFS server on your PC to enable file sharing.\n" +
                        "As a result your PC will become a node in the Inter-Planetary File System that\n" +
                        "enables distributed sharing of information. Before proceeding with IPFS, please\n" +
                        "make sure you understand the full implications of this by getting familiar with\n" +
                        "the details of IPFS at this web site: https://ipfs.io/\n" +
                        "\n" +
                        "The IPFS server needs TCP ports 4001, 5001, 8080 on the system for its own use!\n" +
                        "The IPFS server will be stopped automatically if you quit the Classic Bitcoin wallet. To\n" +
                        "ensure that your contacts can reach the data you share, you may not quit the\n" +
                        "wallet for as long as you expect your contacts to access the data. The data you\n" +
                        "share over IPFS is public - may be accessed by anyone! The IPFS server startup\n" +
                        "may take some seconds so please be patient...\n" +
// TODO: firewalled warning
                        "\n" +
                        "Do you wish to start an IPFS server on your PC?",
                "Confirm starting an IPFS server...",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]);

        if (option == 2) {
            ipfsMessageFlagFile.createNewFile();
        }

        return (option != 1);
    }


    private boolean isIPFSWrapperRunning()
            throws IOException, InterruptedException {
        DaemonInfo info = this.getIPFSDaemonInfo();

        return info.status == DAEMON_STATUS.RUNNING;
    }


    private DaemonInfo getIPFSDaemonInfo()
            throws IOException, InterruptedException {
        OS_TYPE os = OSUtil.getOSType();

        if (os == OS_TYPE.WINDOWS) {
            return CBTCInstallationObserver.getDaemonInfoForWindowsOS("ipfs");
        } else {
            return CBTCInstallationObserver.getDaemonInfoForUNIXLikeOS("ipfs");
        }
    }


    private String getIPFSFullExecutablePath()
            throws IOException {
        return this.getIPFSDirectory() + File.separator + this.getIPFSExecutableName();
    }


    private String getIPFSDirectory()
            throws IOException {
        String walletBase = OSUtil.getProgramDirectory();

        return walletBase + File.separator + "go-ipfs";
    }


    private String getIPFSExecutableName() {
        String ipfs = "ipfs";
        OS_TYPE os = OSUtil.getOSType();
        if (os == OS_TYPE.WINDOWS) {
            ipfs += ".exe";
        }

        return ipfs;
    }


    // Uploads IPFS data via a writable gateway
    // serverURL - like http://localhost:8080/ipfs/
    private boolean uploadIPFSDataViaPost(File f, String serverURL)
            throws MalformedURLException, ProtocolException, IOException {
        URL obj = new URL(serverURL);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        con.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(con.getOutputStream());
        out.write(Util.loadFileInMemory(f));
        out.flush();
        out.close();

        int responseCode = con.getResponseCode();
        // TODO: for now compare hashes until it is clear POST works the same way
        System.out.println("IPFS header fields:" + con.getHeaderFields());

        return responseCode == 201; // Created
    }
}
