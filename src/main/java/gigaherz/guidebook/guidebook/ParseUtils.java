package gigaherz.guidebook.guidebook;

import com.sun.javafx.geom.Vec3f;
import gigaherz.guidebook.GuidebookMod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

/**
 * @author joazlazer
 *
 * Provides a variety of parsing utilities to consolidate common code
 */
public class ParseUtils {
    /**
     * Parses a vector from the input String that is either in the format of 'Xf' or 'Xf,Yf,Zf'
     * @param toParse The input String
     * @return A Vec3f containing the parsed information if valid, and <code>null</code> if parsing failed
     */
    @Nullable
    public static Vec3f parseVec3f(@Nonnull String toParse) {
        try {
            if(toParse.indexOf(',') != -1) {
                // Parse as comment-separated x,y,z vector
                float x = Float.parseFloat(toParse.substring(0, toParse.indexOf(',')));
                float y = Float.parseFloat(toParse.substring(toParse.indexOf(',') + 1, toParse.lastIndexOf(',')));
                float z = Float.parseFloat(toParse.substring(toParse.lastIndexOf(',')));
                return new Vec3f(x, y, z);

            } else {
                // Parse as single-digit cubic vector
                float s = Float.parseFloat(toParse);
                return new Vec3f(s, s, s);
            }
        } catch (NumberFormatException ex) {
            GuidebookMod.logger.warn(String.format("Input Vector3f string '%s' cannot be parsed: %s", toParse, ex.getMessage()));
            return null;
        }
    }

    /**
     * Parses a vector from the input String that is either in the format of 'Xi' or 'Xi,Yi'
     * @param toParse The input String
     * @return A Point containing the parsed information if valid, and <code>null</code> if parsing failed
     */
    @Nullable
    public static Point parsePoint(@Nonnull String toParse) {
        try {
            if(toParse.indexOf(',') != -1) {
                // Parse as comment-separated x,y vector
                int x = Integer.parseInt(toParse.substring(0, toParse.indexOf(',')));
                int y = Integer.parseInt(toParse.substring(toParse.lastIndexOf(',')));
                return new Point(x, y);
            } else {
                // Parse as single-digit square vector
                int s = Integer.parseInt(toParse);
                return new Point(s, s);
            }
        } catch (NumberFormatException ex) {
            GuidebookMod.logger.warn(String.format("Input Point(x,y) string '%s' cannot be parsed: %s", toParse, ex.getMessage()));
            return null;
        }
    }
}
