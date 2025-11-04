import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CustomerStore {

    public CustomerStore() {
        try { CustomerDao.ensureTable(); }
        catch (SQLException e) {
            throw new RuntimeException("Failed to ensure Customer table: " + e.getMessage(), e);
        }
    }

    // digits only
    public static String normalizePhone(String s) {
        if (s == null) return "";
        return s.replaceAll("[^0-9]", "");
    }

    // insert
    public boolean insert(Customer c) {
        try {
            String phone = normalizePhone(c.getPhoneNumber());
            String name  = safe(c.getName()).trim();
            String addr  = safe(c.getAddress()).trim();
            String email = safe(c.getEmail()).trim();

            if (!isValidPhone(phone) || !isValidName(name) || !isValidAddress(addr)) return false;
            if (emailError(email) != null) return false;

            return CustomerDao.insert(new Customer(phone, name, addr, email));
        } catch (SQLException e) { return false; }
    }

    // update
    public boolean update(Customer c) {
        try {
            String phone = normalizePhone(c.getPhoneNumber());
            String name  = safe(c.getName()).trim();
            String addr  = safe(c.getAddress()).trim();
            String email = safe(c.getEmail()).trim();

            if (!isValidPhone(phone) || !isValidName(name) || !isValidAddress(addr)) return false;
            if (emailError(email) != null) return false;

            return CustomerDao.update(new Customer(phone, name, addr, email));
        } catch (SQLException e) { return false; }
    }

    // delete
    public boolean delete(String phoneRaw) {
        try { return CustomerDao.delete(normalizePhone(phoneRaw)); }
        catch (SQLException e) { return false; }
    }

    // find
    public Customer getByPhone(String phoneRaw) {
        try { return CustomerDao.find(normalizePhone(phoneRaw)); }
        catch (SQLException e) { return null; }
    }

    // list
    public List<Customer> listAll() {
        try { return CustomerDao.listAll(); }
        catch (SQLException e) { return List.of(); }
    }

    // csv import
    public String loadFromCsv(String path) { return importCsv(Path.of(path)); }

    public String importCsv(Path csvPath) {
        int added=0, updated=0, skipped=0, total=0;
        try (BufferedReader br = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                total++;
                if (line.isBlank()) { skipped++; continue; }
                List<String> cols = parseCsvLine(line);
                if (cols.size() != 4) { skipped++; continue; }

                String phone = normalizePhone(safe(cols.get(0)).trim());
                String name  = safe(cols.get(1)).trim();
                String addr  = safe(cols.get(2)).trim();
                String email = safe(cols.get(3)).trim();

                if (!isValidPhone(phone) || !isValidName(name) ||
                        !isValidAddress(addr) || emailError(email) != null) {
                    skipped++; continue;
                }

                Customer c = new Customer(phone, name, addr, email);

                if (CustomerDao.insert(c)) added++;
                else if (CustomerDao.update(c)) updated++;
                else skipped++;
            }
        } catch (IOException | SQLException e) {
            return "Import error: " + e.getMessage();
        }
        return "Total: "+total+" | Added: "+added+" | Updated: "+updated+" | Skipped: "+skipped;
    }

    // csv export
    public boolean saveToCsv(String path) {
        try {
            List<Customer> list = CustomerDao.listAll();
            try (BufferedWriter bw = Files.newBufferedWriter(Path.of(path), StandardCharsets.UTF_8)) {
                bw.write("Phone,Name,Address,Email\n");
                for (Customer c : list) {
                    bw.write(csv(c.getPhoneNumber()) + "," +
                            csv(c.getName()) + "," +
                            csv(c.getAddress()) + "," +
                            csv(c.getEmail()) + "\n");
                }
            }
            return true;
        } catch (Exception e) { return false; }
    }

    // email optional
    public static boolean isValidEmail(String s) {
        if (s == null || s.isBlank()) return true;
        return s.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    public static String emailError(String s) {
        if (s == null || s.isBlank()) return null;
        if (!isValidEmail(s)) return "Invalid email. Use format name@example.com.";
        return null;
    }

    // other checks
    public static boolean isValidPhone(String p) { return p != null && p.length() >= 7 && p.length() <= 11; }
    public static boolean isValidName(String s) { return s != null && !s.isBlank(); }
    public static boolean isValidAddress(String s) { return s != null && !s.isBlank(); }
    public static String safe(String s) { return s == null ? "" : s; }

    // csv helpers
    private static String csv(String s) {
        return s == null ? "" : s.contains(",") ? "\"" + s.replace("\"","\"\"") + "\"" : s;
    }
    private static List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean q=false;
        for (char c : line.toCharArray()) {
            if (c=='"') q=!q;
            else if (c==',' && !q) { result.add(sb.toString()); sb.setLength(0); }
            else sb.append(c);
        }
        result.add(sb.toString());
        return result;
    }
}
