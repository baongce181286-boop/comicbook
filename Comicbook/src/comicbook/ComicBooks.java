/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package comicbook;

import java.util.Locale;

/**
 *
 * @author ZAAHEN
 */
public class ComicBooks {
    // Price constraints (must match UI validation)
    public static final double MIN_PRICE = 0.01;
    public static final double MAX_PRICE = 1000.00;

    private int id;
    private String title;
    private double bookRentalPrice; // USD
    private String author;
    private int volume;

    public ComicBooks(int id, String title, double bookRentalPrice, String author, int volume)
            throws ComicBookException {
        setId(id);
        setTitle(title);
        setBookRentalPrice(bookRentalPrice);
        setAuthor(author);
        setVolume(volume);
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public double getBookRentalPrice() { return bookRentalPrice; }
    public String getAuthor() { return author; }
    public int getVolume() { return volume; }

    public void setId(int id) throws ComicBookException {
        if (id <= 0) throw new ComicBookException("ID must be a positive integer.");
        this.id = id;
    }

    public void setTitle(String title) throws ComicBookException {
        if (title == null || title.trim().isEmpty())
            throw new ComicBookException("Title cannot be empty.");
        this.title = title.trim().replaceAll("\\s+", " ");
    }

    public void setBookRentalPrice(double bookRentalPrice) throws ComicBookException {
        if (bookRentalPrice < MIN_PRICE || bookRentalPrice > MAX_PRICE) {
            throw new ComicBookException("Book rental price must be in ["
                    + String.format(Locale.US, "%.2f", MIN_PRICE) + ", "
                    + String.format(Locale.US, "%.2f", MAX_PRICE) + "].");
        }
        this.bookRentalPrice = bookRentalPrice;
    }

    public void setAuthor(String author) throws ComicBookException {
        if (author == null || author.trim().isEmpty())
            throw new ComicBookException("Author cannot be empty.");
        String a = author.trim().replaceAll("\\s+", " ");
        if (a.contains("@")) throw new ComicBookException("Author cannot contain '@'.");
        this.author = a;
    }

    public void setVolume(int volume) throws ComicBookException {
        if (volume <= 0) throw new ComicBookException("Volume must be a positive integer.");
        this.volume = volume;
    }

    // Duplicate content: same Title + Author + Volume (case-insensitive)
    public boolean isSameContent(ComicBooks other) {
        if (other == null) return false;
        return this.title.equalsIgnoreCase(other.title)
                && this.author.equalsIgnoreCase(other.author)
                && this.volume == other.volume;
    }

    // File format: id|title|price|author|volume
    public String toFileLine() {
        return id + "|" + escape(title) + "|" + String.format(Locale.US, "%.2f", bookRentalPrice)
                + "|" + escape(author) + "|" + volume;
    }

    public static ComicBooks fromFileLine(String line) {
        try {
            String[] parts = line.split("\\|", -1);
            if (parts.length != 5) return null;

            int id = Integer.parseInt(parts[0].trim());
            String title = unescape(parts[1]).trim();
            double price = Double.parseDouble(parts[2].trim());
            String author = unescape(parts[3]).trim();
            int volume = Integer.parseInt(parts[4].trim());

            if (id <= 0 || volume <= 0) return null;
            if (title.isEmpty() || author.isEmpty()) return null;

            // price check uses the same constraints
            if (price < MIN_PRICE || price > MAX_PRICE) return null;

            return new ComicBooks(id, title, price, author, volume);
        } catch (Exception e) {
            return null;
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private static String unescape(String s) {
        if (s == null) return "";
        String out = s.replace("\\|", "|");
        out = out.replace("\\\\", "\\");
        return out;
    }
}
