package gigaherz.guidebook.guidebook;

import com.sun.javafx.geom.Vec3f;
import com.sun.javafx.geom.Vec4f;
import gigaherz.guidebook.GuidebookMod;
import net.minecraft.util.math.Vec3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

/**
 * @author joazlazer
 * <p>
 * Provides a variety of parsing utilities to consolidate common code
 */
public class ParseUtils
{
    /**
     * Parses a vector from the input String that is either in the format of 'Xf' or 'Xf,Yf,Zf'
     *
     * @param toParse The input String
     * @return A Vec3f containing the parsed information if valid, or <code>null</code> if parsing failed
     */
    @Nullable
    public static Vec3f parseVec3f(@Nonnull String toParse)
    {
        try
        {
            if (toParse.indexOf(',') != -1)
            {
                // Parse as comma-separated x,y,z vector
                float x = Float.parseFloat(toParse.substring(0, toParse.indexOf(',')));
                float y = Float.parseFloat(toParse.substring(toParse.indexOf(',') + 1, toParse.lastIndexOf(',')));
                float z = Float.parseFloat(toParse.substring(toParse.lastIndexOf(',') + 1));
                return new Vec3f(x, y, z);

            }
            else
            {
                // Parse as single-digit cubic vector
                float s = Float.parseFloat(toParse);
                return new Vec3f(s, s, s);
            }
        } catch (NumberFormatException | StringIndexOutOfBoundsException ex)
        {
            GuidebookMod.logger.warn(String.format("Input Vector3f(x,y,z) string '%s' cannot be parsed: %s", toParse, ex.getMessage()));
            return null;
        }
    }

    /**
     * Parses a vector from the input String that is either in the format of 'Xi' or 'Xi,Yi,Zi'
     *
     * @param toParse The input String
     * @return A Vec3i containing the parsed information if valid, or <code>null</code> if parsing failed
     */
    @Nullable
    public static Vec3i parseVec3i(@Nonnull String toParse)
    {
        try
        {
            if (toParse.indexOf(',') != -1)
            {
                // Parse as comma-separated x,y,z vector
                int x = Integer.parseInt(toParse.substring(0, toParse.indexOf(',')));
                int y = Integer.parseInt(toParse.substring(toParse.indexOf(',') + 1, toParse.lastIndexOf(',')));
                int z = Integer.parseInt(toParse.substring(toParse.lastIndexOf(',') + 1));
                return new Vec3i(x, y, z);

            }
            else
            {
                // Parse as single-digit cubic vector
                int s = Integer.parseInt(toParse);
                return new Vec3i(s, s, s);
            }
        } catch (NumberFormatException | StringIndexOutOfBoundsException ex)
        {
            GuidebookMod.logger.warn(String.format("Input Vector3i(x,y,z) string '%s' cannot be parsed: %s", toParse, ex.getMessage()));
            return null;
        }
    }

    /**
     * Parses a vector from the input String that is either in the format of 'Xf' or 'Xf,Yf,Zf,Wf'
     *
     * @param toParse The input String
     * @return A Vec4f containing the parsed information if valid, or <code>null</code> if parsing failed
     */
    @Nullable
    public static Vec4f parseVec4f(@Nonnull String toParse)
    {
        try
        {
            if (toParse.indexOf(',') != -1)
            {
                // Parse as comma-separated x,y,z,w vector using the array parser
                String[] data = ParseUtils.parseArray("[" + toParse + "]");
                if (data != null)
                {
                    float x = Float.parseFloat(data[0]);
                    float y = Float.parseFloat(data[1]);
                    float z = Float.parseFloat(data[2]);
                    float w = Float.parseFloat(data[3]);
                    return new Vec4f(x, y, z, w);
                }
                else
                {
                    GuidebookMod.logger.warn(String.format("Input Vector4f(x,y,z,w) string '%s' cannot be parsed: %s", toParse, "(See above error)"));
                    return null;
                }
            }
            else
            {
                // Parse as single-digit hyper-cubic vector
                float s = Float.parseFloat(toParse);
                return new Vec4f(s, s, s, s);
            }
        } catch (NumberFormatException | StringIndexOutOfBoundsException ex)
        {
            GuidebookMod.logger.warn(String.format("Input Vector4f(x,y,z,w) string '%s' cannot be parsed: %s", toParse, ex.getMessage()));
            return null;
        }
    }

    /**
     * Parses an array of Strings from the input String that is in the format 'str' or '[str]' or '[str,str...]'
     *
     * @param toParse The input String
     * @return An array of Strings containing elements from the parsed data, or <code>null</code> if parsing failed
     */
    @Nullable
    public static String[] parseArray(@Nonnull String toParse)
    {
        try
        {
            if (toParse.indexOf('[') != -1)
            {
                // Parse as comma-separated array with capping [ and ]
                String insideArea = toParse.substring(toParse.indexOf('[') + 1, toParse.lastIndexOf(']'));
                ArrayList<String> arrayBuilder = new ArrayList<>();
                boolean hasNext = insideArea.trim().length() >= 1;
                while (hasNext)
                {
                    String entry;
                    if (insideArea.indexOf(',') != -1)
                    {
                        // Has more entries after current
                        entry = insideArea.substring(0, insideArea.indexOf(','));
                        insideArea = insideArea.substring(insideArea.indexOf(',') + 1);
                    }
                    else
                    {
                        // Has no more entries after current
                        entry = insideArea;
                        hasNext = false;
                    }
                    if (!entry.isEmpty())
                    {
                        arrayBuilder.add(entry);
                    }
                }
                return arrayBuilder.toArray(new String[arrayBuilder.size()]);
            }
            else
            {
                // Parse as single-entry array with no special characters
                return new String[]{toParse};
            }
        } catch (NumberFormatException | StringIndexOutOfBoundsException ex)
        {
            GuidebookMod.logger.warn(String.format("Input Array[,] string '%s' cannot be parsed: %s", toParse, ex.getMessage()));
            return null;
        }
    }
}
