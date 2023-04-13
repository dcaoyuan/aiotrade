
package TestJavaClient;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

public class SharesAllocDlg extends JDialog {
    JComboBox 	m_cmbAcctCode = new JComboBox();
    JTextField 	m_txtNumShares = new JTextField(10);
    JTextField 	m_txtSharesAllocation = new JTextField(20);
    JButton 	m_btnAddShareAlloc = new JButton("Add Share Allocation");
    JButton 	m_btnOk = new JButton( "OK");
    JButton 	m_btnCancel = new JButton( "Cancel");
    OrderDlg	m_parent;

    String 		m_sharesAllocation;
    String 		m_acctCodes;
    boolean 	m_rc;

    public SharesAllocDlg( OrderDlg owner, String acctCodes) {
        super( owner, true);

        m_parent = owner;
        m_acctCodes = acctCodes;

        setTitle("Shares Allocation Profile");
        setSize(400,400);

     	m_cmbAcctCode.setMaximumSize( m_cmbAcctCode.getPreferredSize());
     	m_txtNumShares.setMaximumSize( m_txtNumShares.getPreferredSize());
     	m_txtSharesAllocation.setMaximumSize( m_txtSharesAllocation.getPreferredSize());

     	// populate the account code combo
     	setAccountCodes();

        Box row1 = Box.createHorizontalBox();
        row1.add( new JLabel( "Account Code :"));	//acctCode Label
        row1.add( Box.createHorizontalStrut(10));	//spacing
        row1.add( m_cmbAcctCode);					//acctCode edit field
        row1.add( Box.createHorizontalStrut(20));	//spacing
        row1.add( new JLabel( "Num of Shares :"));	//number of shares Label
        row1.add( Box.createHorizontalStrut(10));	//spacing
        row1.add( m_txtNumShares);					//numShares edit field
        Box row2 = Box.createHorizontalBox();
        row2.add( m_btnAddShareAlloc);				// add share/acct button


        Box vbox1 = Box.createVerticalBox();
        vbox1.add( Box.createVerticalStrut(10));
        vbox1.add( row1);
        vbox1.add( Box.createVerticalStrut(20));
        vbox1.add( row2);
        vbox1.add( Box.createVerticalStrut(10));
        // create share/alloc entry panel
        JPanel sharesEntryPanel = new JPanel();
        sharesEntryPanel.setBorder( BorderFactory.createTitledBorder( ""));
        sharesEntryPanel.add(vbox1);


        Box row3 = Box.createHorizontalBox();
        row3.add( m_txtSharesAllocation);			// shares profile
        Box vbox2 = Box.createVerticalBox();
        vbox2.add( row3);
        vbox2.add( Box.createVerticalStrut(10));
        // create share/alloc display panel
        JPanel sharesDisplayPanel = new JPanel();
        sharesDisplayPanel.setBorder( BorderFactory.createTitledBorder( "Shares Allocation Details"));
        sharesDisplayPanel.add(vbox2);


        // create button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.add( m_btnOk);
        buttonPanel.add( m_btnCancel);

        // create action listeners
        m_btnAddShareAlloc.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onAddShareAlloc();
            }
        });
        m_btnOk.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onOk();
            }
        });
        m_btnCancel.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e) {
                onCancel();
            }
        });

        // create dlg box
        getContentPane().add( sharesEntryPanel, BorderLayout.NORTH);
        getContentPane().add( sharesDisplayPanel, BorderLayout.CENTER);
        getContentPane().add( buttonPanel, BorderLayout.SOUTH);
        pack();
    }

    void onAddShareAlloc() {
        String sharesProfile = m_txtSharesAllocation.getText().toString();

        if ( sharesProfile.trim().length() > 0) {
            m_txtSharesAllocation.setText( m_txtSharesAllocation.getText() + ",");
        }

        m_txtSharesAllocation.setText( 	m_txtSharesAllocation.getText() +
                                                                    m_cmbAcctCode.getSelectedItem().toString() +
                                                                    "/" +
                                                                    m_txtNumShares.getText());
    }

    void onOk() {
        m_sharesAllocation = m_txtSharesAllocation.getText();
        m_rc = true;
        setVisible( false);
    }

	/**
	 *
	 */
    void onCancel() {
        m_sharesAllocation = "";
        m_rc = false;
        setVisible( false);
    }

    void setAccountCodes() {
        StringTokenizer acctSharePairs = new StringTokenizer( m_acctCodes, ",");

        // Add each account code to the combo
        while (acctSharePairs.hasMoreTokens() ) {
            // Get the account/share allocation pair
            String acctSharePair = acctSharePairs.nextToken();

            // Split them and add the account code to the account combobox
            StringTokenizer st = new StringTokenizer( acctSharePair, "/");
            if ( st.hasMoreTokens() ) {
                m_cmbAcctCode.addItem( st.nextToken());
            }
        }
    }
}
