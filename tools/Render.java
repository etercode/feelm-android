import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Renders every raster asset from the one mark definition, so the launcher
 * tile, the Play listing and the feature graphic can never drift apart.
 * The vector drawables in res/drawable are this same geometry by hand.
 *
 * Run on the Android Studio JBR: tools\run.bat tools\Render.java
 */
public class Render {

    static final String ROOT = "C:/dev/feelm-android";
    static final String RES = ROOT + "/app/src/main/res";
    static final String BRAND = ROOT + "/brand";

    /** The navy launcher tile — one step above the app's DarkPage. */
    static final Color TILE = new Color(0x0D1424);
    static final Color PAGE_DARK = new Color(0x090F1C);
    static final Color INK_DARK = new Color(0xEBEEF5);

    /** Dark-ground palette: arms in brand blue, spine in the four media hues. */
    static final Color ARM_DARK = new Color(0x649FF6);
    static final Color[] SPINE_DARK = {
        new Color(0x649FF6), new Color(0xBF7EE6), new Color(0x33B78F), new Color(0xD48D2C),
    };

    /** Light-ground palette, for the transparent mark used on paper. */
    static final Color ARM_LIGHT = new Color(0x1066D5);
    static final Color[] SPINE_LIGHT = {
        new Color(0x1066D5), new Color(0x913BBD), new Color(0x147E60), new Color(0x935F10),
    };

    /** Everything renders at 4x and downsamples once, for clean edges. */
    static final int SS = 4;

    public static void main(String[] args) throws Exception {
        new File(BRAND).mkdirs();

        // Launcher mipmaps. Dead code on minSdk 26 — the adaptive icon always
        // wins — but the scaffold robot should not stay in the repo.
        int[][] densities = {{48, 0}, {72, 1}, {96, 2}, {144, 3}, {192, 4}};
        String[] names = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
        for (int[] d : densities) {
            String dir = RES + "/mipmap-" + names[d[1]];
            save(tile(d[0], false), dir + "/ic_launcher.png");
            save(tile(d[0], true), dir + "/ic_launcher_round.png");
        }

        // Play listing icon: 512, opaque, square — Play applies its own mask.
        save(opaque(tile512(), TILE), BRAND + "/icon-512.png");

        // Feature graphic: 1024x500, mark + wordmark on the dark page colour.
        save(featureGraphic(), BRAND + "/feature-graphic.png");

        // The bare mark on transparency, one per ground, for reuse anywhere.
        save(bareMark(512, ARM_LIGHT, SPINE_LIGHT), BRAND + "/mark-on-light.png");
        save(bareMark(512, ARM_DARK, SPINE_DARK), BRAND + "/mark-on-dark.png");

        System.out.println("done");
    }

    /**
     * The Spectrum F in a 100-unit box, identical to ic_launcher_foreground:
     * four spine blocks at x18 w22 h17, two pill arms on rows one and two at
     * x44 — nothing touches, everything shares the 4-unit gutter.
     * Ink spans x 18..81, y 10..90.
     */
    static void mark(Graphics2D g, double s, double tx, double ty, Color arm, Color[] spine) {
        AffineTransform saved = g.getTransform();
        g.translate(tx, ty);
        g.scale(s, s);
        for (int i = 0; i < 4; i++) {
            g.setColor(spine[i]);
            g.fill(new RoundRectangle2D.Double(18, 10 + 21 * i, 22, 17, 14, 14));
        }
        g.setColor(arm);
        g.fill(new RoundRectangle2D.Double(44, 10, 37, 17, 17, 17));
        g.fill(new RoundRectangle2D.Double(44, 31, 26, 17, 17, 17));
        g.setTransform(saved);
    }

    /** Centres the mark (bbox centre 49.5, 50) at a given point and height. */
    static void markCentred(Graphics2D g, double cx, double cy, double height,
                            Color arm, Color[] spine) {
        double s = height / 80.0;
        mark(g, s, cx - 49.5 * s, cy - 50 * s, arm, spine);
    }

    /** A navy tile with the mark at 60% height; circle or rounded square. */
    static BufferedImage tile(int size, boolean round) {
        int s = size * SS;
        BufferedImage im = canvas(s, s);
        Graphics2D g = graphics(im);
        g.setColor(TILE);
        if (round) {
            g.fill(new Ellipse2D.Double(0, 0, s, s));
        } else {
            double r = s * 0.44;
            g.fill(new RoundRectangle2D.Double(0, 0, s, s, r, r));
        }
        markCentred(g, s / 2.0, s / 2.0, s * 0.60, ARM_DARK, SPINE_DARK);
        g.dispose();
        return downscale(im, size, size);
    }

    /** Play's 512 wants the full square — it rounds the corners itself. */
    static BufferedImage tile512() {
        int s = 512 * SS;
        BufferedImage im = canvas(s, s);
        Graphics2D g = graphics(im);
        g.setColor(TILE);
        g.fill(new Rectangle2D.Double(0, 0, s, s));
        markCentred(g, s / 2.0, s / 2.0, s * 0.60, ARM_DARK, SPINE_DARK);
        g.dispose();
        return downscale(im, 512, 512);
    }

    /** Mark + wordmark, centred as one lockup on the dark page colour. */
    static BufferedImage featureGraphic() {
        int w = 1024 * 2, h = 500 * 2;
        BufferedImage im = canvas(w, h);
        Graphics2D g = graphics(im);
        g.setColor(PAGE_DARK);
        g.fill(new Rectangle2D.Double(0, 0, w, h));

        double markH = 380;
        double markW = markH / 80.0 * 63.0;
        double gap = 110;

        // Tight tracking to match the app's -0.045em wordmark.
        Font font = new Font("Segoe UI", Font.BOLD, 380)
                .deriveFont(Map.of(TextAttribute.TRACKING, -0.04f));
        TextLayout layout = new TextLayout("feelm", font, g.getFontRenderContext());
        Rectangle2D text = layout.getBounds();

        double total = markW + gap + text.getWidth();
        double left = (w - total) / 2.0;
        double cy = h / 2.0;

        markCentred(g, left + markW / 2.0, cy, markH, ARM_DARK, SPINE_DARK);
        g.setColor(INK_DARK);
        layout.draw(g,
                (float) (left + markW + gap - text.getX()),
                (float) (cy - text.getCenterY()));
        g.dispose();
        return downscale(im, 1024, 500);
    }

    /** The bare mark on transparency, filling 92% of the box height. */
    static BufferedImage bareMark(int size, Color arm, Color[] spine) {
        int s = size * SS;
        BufferedImage im = canvas(s, s);
        Graphics2D g = graphics(im);
        markCentred(g, s / 2.0, s / 2.0, s * 0.92, arm, spine);
        g.dispose();
        return downscale(im, size, size);
    }

    static BufferedImage canvas(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    static Graphics2D graphics(BufferedImage im) {
        Graphics2D g = im.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g;
    }

    static BufferedImage downscale(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    /** Flattens onto a ground: Play rejects listing icons with an alpha story. */
    static BufferedImage opaque(BufferedImage src, Color ground) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(ground);
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    static void save(BufferedImage im, String path) throws Exception {
        File f = new File(path);
        f.getParentFile().mkdirs();
        ImageIO.write(im, "png", f);
        System.out.println(path + "  " + im.getWidth() + "x" + im.getHeight());
    }
}
