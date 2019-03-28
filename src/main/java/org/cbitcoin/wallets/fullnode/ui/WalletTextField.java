package org.cbitcoin.wallets.fullnode.ui;

import javax.swing.*;
import org.cbitcoin.wallets.fullnode.util.Util;
	
public class WalletTextField
        extends JTextField
{
    public WalletTextField(int columns)
    {
        super(columns);
    }
        
    public String getText()
    {
    	return Util.removeUTF8BOM(super.getText());
    }

} // End class