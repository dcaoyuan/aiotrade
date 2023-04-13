
package TestJavaClient;

import java.awt.Component;
import javax.swing.JOptionPane;

public class Main {

    // This method is called to start the application
    public static void main (String args[]) {
        SampleFrame sampleFrame = new SampleFrame();
        sampleFrame.show();
    }

    static public void inform( Component parent, String str) {
        showMsg( parent, str, JOptionPane.INFORMATION_MESSAGE);
    }

    static private void showMsg( Component parent, String str, int type) {
        // this function pops up a dlg box displaying a message
        JOptionPane.showMessageDialog( parent, str, "IB Java Test Client", type);
    }
}
