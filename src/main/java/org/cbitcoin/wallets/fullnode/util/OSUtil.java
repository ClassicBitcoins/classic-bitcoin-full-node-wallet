package org.cbitcoin.wallets.fullnode.util;

import org.cbitcoin.wallets.fullnode.daemon.CommandExecutor;

import java.io.File;
import java.io.IOException;
import java.util.Locale;


/**
 * Utilities - may be OS dependent.
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class OSUtil
{

    public static enum OS_TYPE
    {
        LINUX, WINDOWS, MAC_OS, FREE_BSD, OTHER_BSD, SOLARIS, AIX, OTHER_UNIX, OTHER_OS
    };


    public static boolean isUnixLike(OS_TYPE os)
    {
        return os == OS_TYPE.LINUX || os == OS_TYPE.MAC_OS || os == OS_TYPE.FREE_BSD ||
                os == OS_TYPE.OTHER_BSD || os == OS_TYPE.SOLARIS || os == OS_TYPE.AIX ||
                os == OS_TYPE.OTHER_UNIX;
    }


    public static boolean isHardUnix(OS_TYPE os)
    {
        return os == OS_TYPE.FREE_BSD ||
                os == OS_TYPE.OTHER_BSD || os == OS_TYPE.SOLARIS ||
                os == OS_TYPE.AIX || os == OS_TYPE.OTHER_UNIX;
    }


    public static OS_TYPE getOSType()
    {
        String name = System.getProperty("os.name").toLowerCase(Locale.ROOT);

        if (name.contains("linux"))
        {
            return OS_TYPE.LINUX;
        } else if (name.contains("windows"))
        {
            return OS_TYPE.WINDOWS;
        } else if (name.contains("sunos") || name.contains("solaris"))
        {
            return OS_TYPE.SOLARIS;
        } else if (name.contains("darwin") || name.contains("mac os") || name.contains("macos"))
        {
            return OS_TYPE.MAC_OS;
        } else if (name.contains("free") && name.contains("bsd"))
        {
            return OS_TYPE.FREE_BSD;
        } else if ((name.contains("open") || name.contains("net")) && name.contains("bsd"))
        {
            return OS_TYPE.OTHER_BSD;
        } else if (name.contains("aix"))
        {
            return OS_TYPE.AIX;
        } else if (name.contains("unix"))
        {
            return OS_TYPE.OTHER_UNIX;
        } else
        {
            return OS_TYPE.OTHER_OS;
        }
    }


    // Returns the name of the zcashd server - may vary depending on the OS.
    public static String getZCashd()
    {
        String zcashd = "cbtcd";

        OS_TYPE os = getOSType();
        if (os == OS_TYPE.WINDOWS)
        {
            zcashd += ".exe";
        }

        return zcashd;
    }


    // Returns the name of the cbtc-cli tool - may vary depending on the OS.
    public static String getZCashCli()
    {
        String zcashcli = "cbtc-cli";

        OS_TYPE os = getOSType();
        if (os == OS_TYPE.WINDOWS)
        {
            zcashcli += ".exe";
        }

        return zcashcli;
    }


    // Returns the directory that the wallet program was started from
    public static String getProgramDirectory()
            throws IOException
    {
        // TODO: this way of finding the dir is JAR name dependent - tricky, may not work
        // if program is repackaged as different JAR!
        final String JAR_NAME = "ClassicBitcoinDesktopWallet.jar";
        String cp = System.getProperty("java.class.path");
        if ((cp != null) && (cp.indexOf(File.pathSeparator) == -1) &&
                (cp.endsWith(JAR_NAME)))
        {
            File pd = new File(cp.substring(0, cp.length() - JAR_NAME.length()));

            if (pd.exists() && pd.isDirectory())
            {
                return pd.getCanonicalPath();
            }
        }

        // Try with a full class-path, now containing more libraries
        // This too is very deployment specific
        if (cp.indexOf(File.pathSeparator) != -1)
        {
            String cp2 = cp;
            if (cp2.endsWith(File.pathSeparator))
            {
                cp2 = cp2.substring(0, cp2.length() - 1);
            }

            if (cp2.startsWith(File.pathSeparator))
            {
                cp2 = cp2.substring(1);
            }

            final String CP_JARS = JAR_NAME + File.pathSeparator + "bitcoinj-core-0.14.5.jar" +
                    File.pathSeparator + "sqlite-jdbc-3.21.0.jar";
            if (cp2.endsWith(CP_JARS))
            {
                String cpStart = cp2.substring(0, cp2.length() - CP_JARS.length());
                if (cpStart.endsWith(File.separator))
                {
                    cpStart = cpStart.substring(0, cpStart.length() - 1);
                }
                int startIndex = cpStart.lastIndexOf(File.pathSeparator);
                if (startIndex < 0)
                {
                    startIndex = 0;
                }

                if (cpStart.length() > startIndex)
                {
                    File pd = new File(cpStart.substring(startIndex));
                    return pd.getCanonicalPath();
                }
            }
        }

        // Current dir of the running JVM (expected)
        String userDir = System.getProperty("user.dir");
        if (userDir != null)
        {
            File ud = new File(userDir);

            if (ud.exists() && ud.isDirectory())
            {
                return ud.getCanonicalPath();
            }
        }

        // TODO: tests and more options

        return new File(".").getCanonicalPath();
    }


    public static File getUserHomeDirectory()
            throws IOException
    {
        return new File(System.getProperty("user.home"));
    }


    public static String getBlockchainDirectory()
            throws IOException
    {
        OS_TYPE os = getOSType();

        if (os == OS_TYPE.MAC_OS)
        {
            return new File(System.getProperty("user.home") + "/Library/Application Support/cbtc").getCanonicalPath();
        } else if (os == OS_TYPE.WINDOWS)
        {
            return new File(System.getenv("APPDATA") + "\\cbtc").getCanonicalPath();
        } else
        {
            return new File(System.getProperty("user.home") + "/.cbtc").getCanonicalPath();
        }
    }


    // Directory with program settings to store as well as logging
    public static String getSettingsDirectory()
            throws IOException
    {
        File userHome = new File(System.getProperty("user.home"));
        File dir;
        OS_TYPE os = getOSType();

        if (os == OS_TYPE.MAC_OS)
        {
            dir = new File(userHome, "Library/Application Support/CBitcoinDesktopWallet");
        } else if (os == OS_TYPE.WINDOWS)
        {
            dir = new File(System.getenv("LOCALAPPDATA") + "\\CBitcoinDesktopWallet");
        } else
        {
            dir = new File(userHome.getCanonicalPath() + File.separator + ".cbitcoinDesktopWallet");
        }

        if (!dir.exists())
        {
            if (!dir.mkdirs())
            {
                Log.warning("Could not create settings directory: " + dir.getCanonicalPath());
            }
        }

        return dir.getCanonicalPath();
    }


    public static String getSystemInfo()
            throws IOException, InterruptedException
    {
        OS_TYPE os = getOSType();

        if (os == OS_TYPE.MAC_OS)
        {
            CommandExecutor uname = new CommandExecutor(new String[] { "uname", "-sr" });
            return uname.execute() + "; " +
                    System.getProperty("os.name") + " " + System.getProperty("os.version");
        } else if (os == OS_TYPE.WINDOWS)
        {
            // TODO: More detailed Windows information
            return System.getProperty("os.name");
        } else
        {
            CommandExecutor uname = new CommandExecutor(new String[] { "uname", "-srv" });
            return uname.execute();
        }
    }


    // Can be used to find cbtcd/cbtc-cli if it is not found in the same place as the wallet JAR
    // Null if not found
    public static File findZCashCommand(String command)
            throws IOException
    {
        File f;

        // Try with system property zcash.location.dir - may be specified by caller
        String ZCashLocationDir = System.getProperty("classicbitcoin.location.dir");
        if ((ZCashLocationDir != null) && (ZCashLocationDir.trim().length() > 0))
        {
            f = new File(ZCashLocationDir + File.separator + command);
            if (f.exists() && f.isFile())
            {
                return f.getCanonicalFile();
            }
        }

        OS_TYPE os = getOSType();

        if (isUnixLike(os))
        {
            // The following search directories apply to UNIX-like systems only
            final String dirs[] = new String[]
                    {
                            "/usr/bin/", // Typical Ubuntu
                            "/bin/",
                            "/usr/local/bin/",
                            "/usr/local/classicbitcoin/bin/",
                            "/usr/lib/classicbitcoin/bin/",
                            "/opt/local/bin/",
                            "/opt/local/classicbitcoin/bin/",
                            "/opt/classicbitcoin/bin/"
                    };

            for (String d : dirs)
            {
                f = new File(d + command);
                if (f.exists())
                {
                    return f;
                }
            }

        } else if (os == OS_TYPE.WINDOWS)
        {
            // A probable Windows directory is a ZCash dir in Program Files
            String programFiles = System.getenv("PROGRAMFILES");
            if ((programFiles != null) && (!programFiles.isEmpty()))
            {
                File pf = new File(programFiles);
                if (pf.exists() && pf.isDirectory())
                {
                    File ZDir = new File(pf, "ClassicBitcoin");
                    if (ZDir.exists() && ZDir.isDirectory())
                    {
                        File cf = new File(ZDir, command);
                        if (cf.exists() && cf.isFile())
                        {
                            return cf;
                        }
                    }
                }
            }
        }

        // Try in the current directory
        f = new File("." + File.separator + command);
        if (f.exists() && f.isFile())
        {
            return f.getCanonicalFile();
        }


        // TODO: Try to find it with which/PATH

        return null;
    }
}
