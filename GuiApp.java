import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.awt.Desktop;
import java.util.List;

public class GuiApp extends JFrame {

    // store is created after user selects DB
    private CustomerStore store;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Phone", "Name", "Address", "Email"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    private final JTable table = new JTable(tableModel);
    private final JTextArea log = new JTextArea(5, 80);
    private final JTextField searchField = new JTextField(18);
    private TableRowSorter<DefaultTableModel> sorter;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GuiApp().setVisible(true));
    }

    public GuiApp() {
        super("Maintenance Shop");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);


        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select SQLite Database (.db)");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(this, "No database selected. Exiting.");
            System.exit(0);
        }
        String selectedDb = fc.getSelectedFile().getAbsolutePath();
        ConnectionManager.setDatabasePath(selectedDb);


        store = new CustomerStore();

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { doExit(); }
        });

        setLayout(new BorderLayout(5,5));
        buildToolbar();
        add(buildMainPanel(), BorderLayout.CENTER);

        // log setup
        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void scroll(){ log.setCaretPosition(log.getDocument().getLength()); }
            public void insertUpdate(javax.swing.event.DocumentEvent e){ scroll(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e){ scroll(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e){ scroll(); }
        });

        // first message
        logMsg("[DB] " + selectedDb);

        refreshTable();
    }

    // simple alerts
    private void info(String m){ JOptionPane.showMessageDialog(this, m); }
    private void warn(String m){ JOptionPane.showMessageDialog(this, m, "Warning", JOptionPane.WARNING_MESSAGE); }
    private void logMsg(String m){ log.append(m + "\n"); }

    private Component buildMainPanel() {
        table.setFillsViewportHeight(true);
        table.setRowHeight(22);
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(5,5,5,5));
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        p.add(new JScrollPane(log), BorderLayout.SOUTH);

        // search
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });

        return p;
    }

    private void buildToolbar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);

        tb.add(btn("Load CSV", e -> onLoadCsv()));
        tb.add(btn("Refresh", e -> refreshTable()));
        tb.addSeparator();
        tb.add(btn("Add", e -> onAdd()));
        tb.add(btn("Update", e -> onUpdate()));
        tb.add(btn("Delete", e -> onDelete()));
        tb.add(btn("Export All", e -> onExportCsv()));
        tb.addSeparator();
        tb.add(btn("Clear Log", e -> log.setText("")));
        tb.add(btn("Exit", e -> doExit()));

        tb.add(Box.createHorizontalGlue());
        tb.add(new JLabel("Search: "));
        tb.add(searchField);
        JButton clear=new JButton("✕");
        clear.addActionListener(e->searchField.setText(""));
        tb.add(clear);

        add(tb, BorderLayout.NORTH);
    }

    private JButton btn(String t, java.awt.event.ActionListener a) {
        JButton b = new JButton(t);
        b.addActionListener(a);
        return b;
    }

    // load CSV
    private void onLoadCsv() {
        JFileChooser c = new JFileChooser();
        if (c.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        String msg = store.loadFromCsv(c.getSelectedFile().getAbsolutePath());
        info(msg);
        logMsg("[CSV] " + msg);
        refreshTable();
    }

    // export CSV
    private void onExportCsv() {
        JFileChooser c = new JFileChooser();
        c.setSelectedFile(new File("backup.csv"));
        if (c.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;

        boolean ok = store.saveToCsv(c.getSelectedFile().getAbsolutePath());
        if(ok){
            String path = c.getSelectedFile().getAbsolutePath();
            info("Export OK\nPath: " + path);
            logMsg("[CSV] Exported: " + path);
            try { Desktop.getDesktop().open(c.getSelectedFile()); } catch(Exception ignored){}
        } else {
            warn("Export failed");
            logMsg("[CSV] Export failed");
        }
    }

    // simple form
    private JPanel makeForm(JTextField ph, JTextField nm, JTextField ad, JTextField em){
        JPanel p=new JPanel(new GridLayout(4,2,5,5));
        p.add(new JLabel("Phone (digits only):")); p.add(ph);
        p.add(new JLabel("Name:")); p.add(nm);
        p.add(new JLabel("Address:")); p.add(ad);
        p.add(new JLabel("Email (optional):")); p.add(em);
        return p;
    }

    // add
    private void onAdd() {
        JTextField ph=new JTextField();
        JTextField nm=new JTextField();
        JTextField ad=new JTextField();
        JTextField em=new JTextField();

        JPanel form=makeForm(ph,nm,ad,em);
        if(JOptionPane.showConfirmDialog(this,form,"Add Customer",
                JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) return;

        String phone = CustomerStore.normalizePhone(ph.getText());
        String name  = nm.getText().trim();
        String addr  = ad.getText().trim();
        String email = em.getText().trim();

        if(!CustomerStore.isValidPhone(phone) || name.isEmpty() || addr.isEmpty()){
            warn("Phone must be 7–11 digits. Name and Address required.");
            return;
        }
        String emailErr = CustomerStore.emailError(email);
        if (emailErr != null) { warn(emailErr); return; }
        if (store.getByPhone(phone) != null) { warn("Phone already exists."); return; }

        if (!store.insert(new Customer(phone,name,addr,email))) { warn("Insert failed."); return; }
        logMsg("[ADD] " + phone + " | " + name);
        refreshTable();
    }

    // update
    private void onUpdate() {
        int r = table.getSelectedRow();
        if(r<0){ warn("Select row"); return;}

        int m = table.convertRowIndexToModel(r);
        String phone = tableModel.getValueAt(m,0).toString();
        String name  = tableModel.getValueAt(m,1).toString();
        String addr  = tableModel.getValueAt(m,2).toString();
        String email = tableModel.getValueAt(m,3)==null?"":tableModel.getValueAt(m,3).toString();

        JTextField ph=new JTextField(phone); ph.setEditable(false);
        JTextField nm=new JTextField(name);
        JTextField ad=new JTextField(addr);
        JTextField em=new JTextField(email);

        JPanel form=makeForm(ph,nm,ad,em);
        if(JOptionPane.showConfirmDialog(this,form,"Edit Customer",
                JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) return;

        String newName=nm.getText().trim();
        String newAddr=ad.getText().trim();
        String newEmail=em.getText().trim();

        if(newName.isEmpty()||newAddr.isEmpty()){
            warn("Name and Address required.");
            return;
        }
        String emailErr = CustomerStore.emailError(newEmail);
        if (emailErr != null) { warn(emailErr); return; }

        if(!store.update(new Customer(phone,newName,newAddr,newEmail))){
            warn("Update failed.");
            return;
        }
        logMsg("[UPDATE] " + phone + " | " + newName);
        refreshTable();
    }

    // delete
    private void onDelete() {
        int r=table.getSelectedRow();
        if(r<0){ warn("Select row"); return;}

        int m = table.convertRowIndexToModel(r);
        String ph = CustomerStore.normalizePhone(tableModel.getValueAt(m,0).toString());

        if(JOptionPane.showConfirmDialog(this,"Delete?","Confirm",
                JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){
            if (store.delete(ph)) {
                logMsg("[DELETE] " + ph);
            } else {
                logMsg("[DELETE] failed " + ph);
            }
            refreshTable();
        }
    }

    // refresh table
    private void refreshTable() {
        List<Customer> rows=store.listAll();
        tableModel.setRowCount(0);
        for(Customer c:rows){
            tableModel.addRow(new Object[]{c.getPhoneNumber(),c.getName(),c.getAddress(),c.getEmail()});
        }
        logMsg("[REFRESH] rows=" + rows.size());
    }

    private void doExit(){
        if(JOptionPane.showConfirmDialog(this,"Exit?","Confirm",
                JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){
            System.exit(0);
        }
    }

    // search
    private void filter(){
        String t=searchField.getText().trim();
        if(t.isEmpty()) {
            sorter.setRowFilter(null);
            logMsg("[SEARCH] cleared");
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)"+t));
            logMsg("[SEARCH] '" + t + "'");
        }
    }
}
