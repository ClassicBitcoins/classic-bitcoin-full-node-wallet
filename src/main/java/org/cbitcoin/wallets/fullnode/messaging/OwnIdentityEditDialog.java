package org.cbitcoin.wallets.fullnode.messaging;

import org.cbitcoin.wallets.fullnode.util.Log;
import org.cbitcoin.wallets.fullnode.util.StatusUpdateErrorReporter;
import org.cbitcoin.wallets.fullnode.util.Util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;


/**
 * Dialog used to edit one's own identity
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class OwnIdentityEditDialog
    extends IdentityInfoDialog {
  private MessagingStorage storage;
  private StatusUpdateErrorReporter errorReporter;

  private static final String LOCAL_MSG_MY_MSG_ID = Util.local("LOCAL_MSG_MY_MSG_ID");
  private static final String LOCAL_MSG_MY_MSG_ID_INFO = Util.local("LOCAL_MSG_MY_MSG_ID_INFO");
  private static final String LOCAL_MSG_SAVE_CLOSE = Util.local("LOCAL_MSG_SAVE_CLOSE");
  private static final String LOCAL_MSG_NICKNAME_REQ = Util.local("LOCAL_MSG_NICKNAME_REQ");
  private static final String LOCAL_MSG_NICKNAME_REQ_DETAIL = Util.local("LOCAL_MSG_NICKNAME_REQ_DETAIL");

  public OwnIdentityEditDialog(JFrame parent, MessagingIdentity identity,
                               MessagingStorage storage, StatusUpdateErrorReporter errorReporter, boolean identityIsBeingCreated) {
    super(parent, identity);

    this.storage = storage;
    this.errorReporter = errorReporter;

    this.setTitle(LOCAL_MSG_MY_MSG_ID);

    this.infoLabel.setText(
        "<html><span style=\"font-size:0.97em;\">" + LOCAL_MSG_MY_MSG_ID_INFO + "</span>");

    nicknameTextField.setEditable(true);
    firstnameTextField.setEditable(true);
    middlenameTextField.setEditable(true);
    surnameTextField.setEditable(true);
    emailTextField.setEditable(true);
    streetaddressTextField.setEditable(true);
    facebookTextField.setEditable(true);
    twitterTextField.setEditable(true);

    // Build the save and Cancel buttons
    if (identityIsBeingCreated) {
      // If the identity is being created only save is allowed
      this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
      this.buttonPanel.removeAll();
    }

    JButton saveButton = new JButton(LOCAL_MSG_SAVE_CLOSE);
    buttonPanel.add(saveButton);
    saveButton.addActionListener(e -> {
      try {
        // Check for validity and save the data - T/Z addresses are not changed!
        String nick = OwnIdentityEditDialog.this.nicknameTextField.getText();
        if ((nick == null) || nick.trim().length() <= 0) {
          JOptionPane.showMessageDialog(
              OwnIdentityEditDialog.this.parentFrame,
              LOCAL_MSG_NICKNAME_REQ_DETAIL,
              LOCAL_MSG_NICKNAME_REQ, JOptionPane.ERROR_MESSAGE);
          return;
        }

        // TODO: check validity of fields to avoid entering rubbish (e.g. invalid e-mail)

        // Save all identity fields from the text fields
        MessagingIdentity id = OwnIdentityEditDialog.this.identity;
        id.setNickname(OwnIdentityEditDialog.this.nicknameTextField.getText());
        id.setFirstname(OwnIdentityEditDialog.this.firstnameTextField.getText());
        id.setMiddlename(OwnIdentityEditDialog.this.middlenameTextField.getText());
        id.setSurname(OwnIdentityEditDialog.this.surnameTextField.getText());
        id.setEmail(OwnIdentityEditDialog.this.emailTextField.getText());
        id.setStreetaddress(OwnIdentityEditDialog.this.streetaddressTextField.getText());
        id.setFacebook(OwnIdentityEditDialog.this.facebookTextField.getText());
        id.setTwitter(OwnIdentityEditDialog.this.twitterTextField.getText());

        // Save the identity
        OwnIdentityEditDialog.this.storage.updateOwnIdentity(id);

        OwnIdentityEditDialog.this.setVisible(false);
        OwnIdentityEditDialog.this.dispose();
      } catch (Exception ex) {
        Log.error("Unexpected error editing My Messaging Identity!", ex);
        OwnIdentityEditDialog.this.errorReporter.reportError(ex, false);
      }
    });

    this.pack();
    this.setLocation(100, 100);
    this.setLocationRelativeTo(parent);
  }

} // End public class OwnIdentityEditDialog
