package org.cbitcoin.wallets.fullnode.messaging;


import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;
import org.cbitcoin.wallets.fullnode.util.Util;

import java.io.*;
import java.util.Date;


/**
 * Encapsulates a zen message
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class Message
{
    public static enum DIRECTION_TYPE
    {
        SENT, RECEIVED
    };

    public static enum VERIFICATION_TYPE
    {
        UNVERIFIED, VERIFICATION_OK, VERIFICATION_FAILED
    };

    // Wire protocol fields
    private Integer version;
    private String  from;
    private String  message;
    private String  sign;
    private String  threadID; // Thread ID for anonymous messages
    private String  returnAddress; // for anonymous messages

    // Additional internal fields - not to be used when transmitted over the wire
    private String            transactionID;
    private Date              time;
    private DIRECTION_TYPE    direction;
    private VERIFICATION_TYPE verification;
    private boolean           isAnonymous; // If the message is sent or received anonymously


    public Message(JsonObject obj)
    {
        this.copyFromJSONObject(obj);
    }


    public Message(File f)
            throws IOException
    {
        Reader r = null;

        try
        {
            r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
            JsonObject obj = Util.parseJsonObject(r);

            this.copyFromJSONObject(obj);
        } finally
        {
            if (r != null)
            {
                r.close();
            }
        }
    }


    public void copyFromJSONObject(JsonObject obj)
    {
        // Wire protocol fields
        this.version       = obj.getInt("ver",              1);
        this.from          = obj.getString("from",          "");
        this.message       = obj.getString("message",       "");
        this.sign          = obj.getString("sign",          "");
        this.threadID      = obj.getString("threadid",      "");
        this.returnAddress = obj.getString("returnaddress", "");

        // Additional fields - may be missing, get default values
        this.transactionID = obj.getString("transactionID", "");
        this.time          = new Date(obj.getLong("time",   0));
        this.direction     = DIRECTION_TYPE.valueOf(
                obj.getString("direction", DIRECTION_TYPE.RECEIVED.toString()));
        this.verification  = VERIFICATION_TYPE.valueOf(
                obj.getString("verification", VERIFICATION_TYPE.UNVERIFIED.toString()));

        if (obj.get("isanonymous") != null)
        {
            this.isAnonymous = obj.getBoolean("isanonymous", false);
        } else
        {
            // Determine from content if it is anonymous
            this.isAnonymous = obj.get("threadid") != null;
        }
    }


    public JsonObject toJSONObject(boolean forWireProtocol)
    {
        JsonObject obj = new JsonObject();

        if (this.isAnonymous())
        {
            obj.set("ver",           version);
            obj.set("message",       nonNull(message));
            obj.set("threadid",      nonNull(threadID));
        } else
        {
            obj.set("ver",           version);
            obj.set("from",          nonNull(from));
            obj.set("message",       nonNull(message));
            obj.set("sign",          nonNull(sign));
        }

        if (!forWireProtocol)
        {
            obj.set("transactionID", nonNull(transactionID));
            obj.set("time",          this.time.getTime());
            obj.set("direction",     this.direction.toString());
            obj.set("verification",  this.verification.toString());
            obj.set("isanonymous",   isAnonymous);
        }

        return obj;
    }


    public void writeToFile(File f)
            throws IOException
    {
        Writer w = null;

        try
        {
            w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
            w.write(this.toJSONObject(false).toString(WriterConfig.PRETTY_PRINT));
        } finally
        {
            if (w != null)
            {
                w.close();
            }
        }
    }


    public Integer getVersion()
    {
        return version;
    }


    public void setVersion(Integer version)
    {
        this.version = version;
    }


    public String getFrom()
    {
        return from;
    }


    public void setFrom(String from)
    {
        this.from = from;
    }


    public String getMessage()
    {
        return message;
    }


    public void setMessage(String message)
    {
        this.message = message;
    }


    public String getSign()
    {
        return sign;
    }


    public void setSign(String sign)
    {
        this.sign = sign;
    }


    public String getTransactionID()
    {
        return transactionID;
    }


    public void setTransactionID(String transactionID)
    {
        this.transactionID = transactionID;
    }


    public Date getTime()
    {
        return time;
    }


    public void setTime(Date time)
    {
        this.time = time;
    }


    public DIRECTION_TYPE getDirection()
    {
        return direction;
    }


    public void setDirection(DIRECTION_TYPE direction)
    {
        this.direction = direction;
    }


    public VERIFICATION_TYPE getVerification()
    {
        return verification;
    }


    public void setVerification(VERIFICATION_TYPE verification)
    {
        this.verification = verification;
    }


    public boolean isAnonymous()
    {
        return isAnonymous;
    }


    public void setAnonymous(boolean isAnonymous)
    {
        this.isAnonymous = isAnonymous;
    }


    public String getThreadID()
    {
        return threadID;
    }


    public void setThreadID(String threadID)
    {
        this.threadID = threadID;
    }


    public String getReturnAddress()
    {
        return returnAddress;
    }


    public void setReturnAddress(String returnAddress)
    {
        this.returnAddress = returnAddress;
    }


    /**
     * Verifies if a message represented as a JSON object is valid according to the
     * ZEN messaging protocol:
     * https://github.com/ZencashOfficial/messaging-protocol/blob/master/README.md
     *
     * @param message
     *
     * @return true if a message represented as a JSON object is valid according to the
     * ZEN messaging protocol.
     */
    public static boolean isValidZENMessagingProtocolMessage(JsonObject message)
    {
        if ((message == null) || (message.isEmpty()))
        {
            return false;
        }

        if (message.get("threadid") != null)
        {
            // Verify anonymous message
            int version          = message.getInt("ver",              -1);
            String msg           = message.getString("message",       "");
            String threadID      = message.getString("threadid",      "");

            return (version > 0)                   &&
                    (!Util.stringIsEmpty(threadID)) &&
                    (!Util.stringIsEmpty(msg));
        } else
        {
            // Verify normal message
            int version          = message.getInt("ver",              -1);
            String from          = message.getString("from",          "");
            String msg           = message.getString("message",       "");
            String sign          = message.getString("sign",          "");

            return (version > 0)               &&
                    (!Util.stringIsEmpty(from)) &&
                    (!Util.stringIsEmpty(msg))  &&
                    (!Util.stringIsEmpty(sign));
        }
    }


    public String nonNull(String s)
    {
        return (s != null) ? s : "";
    }
}