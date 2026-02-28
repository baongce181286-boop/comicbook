/* e32efqf2qf
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comicbook;

/**
 *
 * @author ZAAHEN
 */
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

public class ComicBookManagement {
    private static final String FILE_NAME = "ComicBooks.txt";

    // UI header required by spec/bugs list
    private static final String SHOP_HEADER = "COMIC BOOK RENTAL SHOP # SE2006-Group06";
    private static final String CLASS_INFO  = "Class: SE2006";
    private static final String LINE = "==================================================";

    // Price constraints
    private static final double MIN_PRICE = 0.01;
    private static final double MAX_PRICE = 1000.00;

    private final ArrayList<ComicBooks> comicBooks = new ArrayList<>();
    private File dataFile; // resolved by user.dir

    public static void main(String[] args) throws ComicBookException {
        Locale.setDefault(Locale.US);
        new ComicBookManagement().run();
    }

    private void run() throws ComicBookException {
        resolveDataFile();
        ensureSampleFileIfMissing();   // auto-create 5 sample books if file missing
        loadFromFile();
        displayInventory();            // show list immediately

        Scanner sc = new Scanner(System.in);
        while (true) {
            printMenu();
            int choice = readInt(sc, "Please select a function:\t", 1, 6);

            switch (choice) {
                case 1:
                    addComicBook(sc);
                    break;
                case 2:
                    searchByTitle(sc);
                    break;
                case 3:
                    searchByAuthor(sc);
                    break;
                case 4:
                    updateRentalPrice(sc);
                    break;
                case 5:
                    deleteById(sc);
                    break;
                case 6:
                    saveToFile();
                    System.out.println("Saved to " + dataFile.getAbsolutePath() + ". Program exited.");
                    sc.close();
                    return;
            }
        }
    }

    private void resolveDataFile() {
        // NetBeans-friendly: uses current working directory (user.dir)
        String dir = System.getProperty("user.dir");
        dataFile = new File(dir, FILE_NAME);

        System.out.println("INFO: Working directory = " + dir);
        System.out.println("INFO: Data file path     = " + dataFile.getAbsolutePath());
    }

    private void ensureSampleFileIfMissing() {
        if (dataFile.exists()) return;

        System.out.println("INFO: " + FILE_NAME + " not found. Creating a sample file with 5 books...");

        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(dataFile), StandardCharsets.UTF_8))) {

            // Header line (will be ignored on load if not in data format)
            bw.write(SHOP_HEADER + " | " + CLASS_INFO);
            bw.newLine();

            bw.write("1|One Piece|2.50|Eiichiro Oda|1"); bw.newLine();
            bw.write("2|Naruto|2.00|Masashi Kishimoto|5"); bw.newLine();
            bw.write("3|Detective Conan|1.80|Gosho Aoyama|10"); bw.newLine();
            bw.write("4|Dragon Ball|2.20|Akira Toriyama|3"); bw.newLine();
            bw.write("5|Attack on Titan|3.00|Hajime Isayama|2"); bw.newLine();

        } catch (Exception e) {
            System.out.println("WARNING: Failed to create sample file.");
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println(SHOP_HEADER);
        System.out.println(CLASS_INFO);
        System.out.println(LINE);
        System.out.println("1. Add new comic book.");
        System.out.println("2. Search book by title.");
        System.out.println("3. Search book of an author.");
        System.out.println("4. Update book rental price.");
        System.out.println("5. Delete comic book.");
        System.out.println("6. Quit.");
        System.out.println(LINE);
    }

    // 1) Add
    private void addComicBook(Scanner sc) {
        System.out.println("\n--- Add new comic book ---");

        if (comicBooks.isEmpty()) {
            // still OK to add when empty; message is for UI friendliness
            System.out.println("(Inventory is currently empty.)");
        }

        int id;
        while (true) {
            id = readInt(sc, "Enter ID (positive integer): ", 1, Integer.MAX_VALUE);
            if (findIndexById(id) != -1) {
                System.out.println("ERROR: ID already exists. Please enter a different ID.");
                continue;
            }
            break;
        }

        String title = readNonEmptyString(sc, "Enter title: ");
        String author = readAuthorString(sc, "Enter author: ");
        int volume = readInt(sc, "Enter volume (positive integer): ", 1, Integer.MAX_VALUE);
        double price = readPrice(sc, "Enter book rental price (USD): ");

        try {
            ComicBooks newBook = new ComicBooks(id, title, price, author, volume);

            // Duplicate content rule: same title+author+volume
            for (ComicBooks b : comicBooks) {
                if (b.isSameContent(newBook)) {
                    System.out.println("ERROR: Duplicate content (same title + author + volume). Not added.");
                    return;
                }
            }

            comicBooks.add(newBook);
            System.out.println("SUCCESS: Comic book added.");
            displayInventory();

        } catch (ComicBookException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    // 2) Search by title
    private void searchByTitle(Scanner sc) {
        System.out.println("\n--- Search book by title ---");

        if (comicBooks.isEmpty()) {
            System.out.println("There is no comic book in the inventory.");
            return;
        }

        System.out.print("Enter title keyword (press Enter to show all): ");
        String query = normalizeKeyword(sc.nextLine());

        if (query.isEmpty()) {
            displayInventory();
            return;
        }

        ArrayList<ComicBooks> matches = new ArrayList<>();
        String q = query.toLowerCase();
        for (ComicBooks b : comicBooks) {
            if (b.getTitle().toLowerCase().contains(q)) {
                matches.add(b);
            }
        }

        if (matches.isEmpty()) {
            System.out.println("No comic book found.");
            return;
        }

        printTable(matches, "SEARCH RESULT");
    }

    // 3) Search by author
    private void searchByAuthor(Scanner sc) {
        System.out.println("\n--- Search book of an author ---");

        if (comicBooks.isEmpty()) {
            System.out.println("There is no comic book in the inventory.");
            return;
        }

        System.out.print("Enter author keyword (press Enter to show all): ");
        String query = normalizeKeyword(sc.nextLine());

        if (query.isEmpty()) {
            displayInventory();
            return;
        }

        ArrayList<ComicBooks> matches = new ArrayList<>();
        String q = query.toLowerCase();
        for (ComicBooks b : comicBooks) {
            if (b.getAuthor().toLowerCase().contains(q)) {
                matches.add(b);
            }
        }

        if (matches.isEmpty()) {
            System.out.println("No comic book found.");
            return;
        }

        printTable(matches, "SEARCH RESULT");
    }

    // 4) Update rental price by ID
    private void updateRentalPrice(Scanner sc) {
        System.out.println("\n--- Update book rental price ---");

        if (comicBooks.isEmpty()) {
            System.out.println("There is no comic book in the inventory.");
            return;
        }

        // Must show table before entering ID
        displayInventory();

        int id = readInt(sc, "Enter ID to update price: ", 1, Integer.MAX_VALUE);
        int idx = findIndexById(id);

        if (idx == -1) {
            System.out.println("ERROR: No comic book found with ID " + id + ".");
            return;
        }

        ComicBooks b = comicBooks.get(idx);
        System.out.println("Current book: " + b.getTitle()
                + " | Current price: " + String.format(Locale.US, "%.2f", b.getBookRentalPrice()) + " $");

        double newPrice = readPrice(sc, "Enter new book rental price (USD): ");

        try {
            b.setBookRentalPrice(newPrice);
            System.out.println("SUCCESS: Rental price updated.");
        } catch (ComicBookException e) {
            System.out.println("ERROR: " + e.getMessage());
            return;
        }

        // Must show table after updating
        displayInventory();
    }

    // 5) Delete by ID
    private void deleteById(Scanner sc) {
        System.out.println("\n--- Delete comic book ---");

        if (comicBooks.isEmpty()) {
            System.out.println("There is no comic book in the inventory.");
            return;
        }

        // Must show table before choosing delete
        displayInventory();

        int id = readInt(sc, "Enter ID to delete: ", 1, Integer.MAX_VALUE);
        int idx = findIndexById(id);

        if (idx == -1) {
            System.out.println("ERROR: No comic book found with ID " + id + ".");
            return;
        }

        comicBooks.remove(idx);
        System.out.println("SUCCESS: Comic book deleted.");

        // Show updated table
        displayInventory();
    }

    // Display list
    private void displayInventory() {
        if (comicBooks.isEmpty()) {
            System.out.println("\nThere is no comic book in the inventory.");
            return;
        }
        printTable(comicBooks, "INVENTORY");
    }

    // ===========================
    // ONLY CHANGED PART: PRINT TABLE
    // ===========================
    private void printTable(ArrayList<ComicBooks> list, String title) {
        System.out.println("\n--- " + title + " ---");

        // widths theo format bảng bạn gửi
        final int W_NO = 4;
        final int W_ID = 10;
        final int W_TITLE = 27;
        final int W_PRICE = 17;   // header: "Book Rental Price"
        final int W_AUTHOR = 22;
        final int W_VOLUME = 6;

        String border = "+"
                + repeat("-", W_NO + 2) + "+"
                + repeat("-", W_ID + 2) + "+"
                + repeat("-", W_TITLE + 2) + "+"
                + repeat("-", W_PRICE + 2) + "+"
                + repeat("-", W_AUTHOR + 2) + "+"
                + repeat("-", W_VOLUME + 2) + "+";

        System.out.println(border);
        System.out.printf("| %-" + W_NO + "s | %-" + W_ID + "s | %-" + W_TITLE + "s | %-" + W_PRICE + "s | %-" + W_AUTHOR + "s | %-" + W_VOLUME + "s |%n",
                "No.", "ID", "Title", "Book Rental Price", "Author", "Volume");
        System.out.println(border);

        int no = 1;
        for (ComicBooks b : list) {
    double price = b.getBookRentalPrice();
    String priceText;

    if (price % 1 == 0) {
        priceText = String.format(Locale.US, "%.0f $", price);   // số nguyên -> không .00
    } else {
        priceText = String.format(Locale.US, "%.2f $", price);   // số thập phân -> in bình thường
    }

    System.out.printf("| %" + W_NO + "d | %" + W_ID + "d | %-" + W_TITLE + "s | %" + W_PRICE + "s | %-" + W_AUTHOR + "s | %" + W_VOLUME + "d |%n",
            no++,
            b.getId(),
            cut(b.getTitle(), W_TITLE),
            priceText,
            cut(b.getAuthor(), W_AUTHOR),
            b.getVolume());
}

        System.out.println(border);
    }

    private String cut(String s, int max) {
        if (s == null) return "";
        s = s.trim().replaceAll("\\s+", " ");
        if (s.length() <= max) return s;
        if (max <= 3) return s.substring(0, max);
        return s.substring(0, max - 3) + "...";
    }

    private String repeat(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) sb.append(str);
        return sb.toString();
    }
    // ===========================
    // END CHANGED PART
    // ===========================

    private int findIndexById(int id) {
        for (int i = 0; i < comicBooks.size(); i++) {
            if (comicBooks.get(i).getId() == id) return i;
        }
        return -1;
    }

    // Load file
    private void loadFromFile() {
        comicBooks.clear();

        if (!dataFile.exists()) {
            System.out.println("INFO: File still not found. Starting empty.");
            return;
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Only parse valid data lines
                if (!line.contains("|")) continue;

                ComicBooks b = ComicBooks.fromFileLine(line);
                if (b == null) continue;

                // unique by ID
                if (findIndexById(b.getId()) != -1) continue;

                // avoid duplicate content
                boolean dupContent = false;
                for (ComicBooks x : comicBooks) {
                    if (x.isSameContent(b)) {
                        dupContent = true;
                        break;
                    }
                }
                if (!dupContent) comicBooks.add(b);
            }

            System.out.println("INFO: Loaded " + comicBooks.size() + " comic book(s) from file.");
        } catch (Exception e) {
            System.out.println("WARNING: Failed to load file.");
        }
    }

    // Save file
    private void saveToFile() {
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(dataFile), StandardCharsets.UTF_8))) {

            // Header line for the required app name
            bw.write(SHOP_HEADER + " | " + CLASS_INFO);
            bw.newLine();

            for (ComicBooks b : comicBooks) {
                bw.write(b.toFileLine());
                bw.newLine();
            }
        } catch (Exception e) {
            System.out.println("WARNING: Failed to save file.");
        }
    }

    // Validation input
    private int readInt(Scanner sc, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String raw = sc.nextLine().trim();

            if (raw.isEmpty()) {
                if (min == 1 && max == 6) {
                    System.out.println("ERROR: Please choose a number from 1 to 6.");
                } else {
                    System.out.println("ERROR: Input cannot be empty. Please enter an integer in [" + min + ", " + max + "].");
                }
                continue;
            }

            try {
                int val = Integer.parseInt(raw);
                if (val < min || val > max) {
                    if (min == 1 && max == 6) {
                        System.out.println("ERROR: Please choose a number from 1 to 6.");
                    } else {
                        System.out.println("ERROR: Value must be in [" + min + ", " + max + "].");
                    }
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                if (min == 1 && max == 6) {
                    System.out.println("ERROR: Please choose a number from 1 to 6.");
                } else {
                    System.out.println("ERROR: Please enter a valid integer.");
                }
            }
        }
    }

    private double readDouble(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = sc.nextLine().trim();
            if (raw.isEmpty()) {
                System.out.println("ERROR: Input cannot be empty.");
                continue;
            }
            try {
                return Double.parseDouble(raw);
            } catch (NumberFormatException e) {
                System.out.println("ERROR: Please enter a valid number.");
            }
        }
    }

    private double readPrice(Scanner sc, String prompt) {
        while (true) {
            double p = readDouble(sc, prompt);
            if (p < MIN_PRICE || p > MAX_PRICE) {
                System.out.println("ERROR: Book rental price must be in ["
                        + String.format(Locale.US, "%.2f", MIN_PRICE) + ", "
                        + String.format(Locale.US, "%.2f", MAX_PRICE) + "].");
                continue;
            }
            return p;
        }
    }

    private String readNonEmptyString(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine();
            s = (s == null) ? "" : s.trim();
            if (s.isEmpty()) {
                System.out.println("ERROR: This field cannot be empty.");
                continue;
            }
            return normalizeKeyword(s);
        }
    }

    // Author must not contain '@'
    private String readAuthorString(Scanner sc, String prompt) {
        while (true) {
            String a = readNonEmptyString(sc, prompt);
            if (a.contains("@")) {
                System.out.println("ERROR: Author cannot contain '@'. Please enter a valid author name.");
                continue;
            }
            return a;
        }
    }

    // Trim + remove extra spaces inside (keyword requirement)
    private String normalizeKeyword(String s) {
        if (s == null) return "";
        String out = s.trim();
        out = out.replaceAll("\\s+", " ");
        return out;
    }
}





