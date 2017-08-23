package gigaherz.guidebook.guidebook;

import gigaherz.guidebook.GuidebookMod;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.common.model.TRSRTransformation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.util.ArrayList;

/**
 * @author joazlazer
 * <p>
 * Provides a variety of parsing utilities to consolidate common code
 */
@SuppressWarnings("WeakerAccess")
public class ParseUtils
{
    /**
     * Parses a vector from the input String that is either in the format of 'Xf' or 'Xf,Yf,Zf'
     *
     * @param toParse The input String
     * @return A Vector3f containing the parsed information if valid, or <code>null</code> if parsing failed
     */
    @Nullable
    public static Vector3f parseVector3f(@Nonnull String toParse)
    {
        try
        {
            if (toParse.indexOf(',') != -1)
            {
                // Parse as comma-separated x,y,z vector
                float x = Float.parseFloat(toParse.substring(0, toParse.indexOf(',')));
                float y = Float.parseFloat(toParse.substring(toParse.indexOf(',') + 1, toParse.lastIndexOf(',')));
                float z = Float.parseFloat(toParse.substring(toParse.lastIndexOf(',') + 1));
                return new Vector3f(x, y, z);

            }
            else
            {
                // Parse as single-digit cubic vector
                float s = Float.parseFloat(toParse);
                return new Vector3f(s, s, s);
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
        }
        catch (NumberFormatException | StringIndexOutOfBoundsException ex)
        {
            GuidebookMod.logger.warn(String.format("Input Vector3i(x,y,z) string '%s' cannot be parsed: %s", toParse, ex.getMessage()));
            return null;
        }
    }

    /**
     * Parses a vector from the input String that is either in the format of 'Xf' or 'Xf,Yf,Zf,Wf'
     *
     * @param toParse The input String
     * @return A Vector4f containing the parsed information if valid, or <code>null</code> if parsing failed
     */
    @Nullable
    public static Vector4f parseVector4f(@Nonnull String toParse)
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
                    return new Vector4f(x, y, z, w);
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
                return new Vector4f(s, s, s, s);
            }
        }
        catch (NumberFormatException | StringIndexOutOfBoundsException | ArrayIndexOutOfBoundsException ex)
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
                        arrayBuilder.add(entry.trim());
                    }
                }
                return arrayBuilder.toArray(new String[arrayBuilder.size()]);
            }
            else
            {
                // Parse as single-entry array with no special characters
                return new String[]{toParse};
            }
        }
        catch (NumberFormatException | StringIndexOutOfBoundsException ex)
        {
            GuidebookMod.logger.warn(String.format("Input Array[,] string '%s' cannot be parsed: %s", toParse, ex.getMessage()));
            return null;
        }
    }

    /**
     * Parses a TRSRTransformation from XML string that ignores right handed rotation
     * Format: t[Xf,Yf,Zf] r[θxf,θyf,θzf] s[Xf,Yf,Zf]
     * OR      t[Xf,Yf,Zf] q[Xf,Yf,Zf,Wf] s[Xf,Yf,Zf]
     *
     * @param toParse The input String
     * @return A new TRSRTransformation instance with the specified information, or <code>null</code> if parsing failed
     */
    @Nullable
    public static TRSRTransformation parseTRSR(@Nonnull String toParse)
    {
        try
        {
            Quat4f rotateQuat = new Quat4f();
            Vector3f translateVec = new Vector3f();
            Vector3f scaleVec = new Vector3f(1f, 1f, 1f);

            // Parse translation
            if (toParse.indexOf('t') != -1)
            {
                String translation = toParse.substring(toParse.indexOf('t') + 2, toParse.indexOf(']', toParse.indexOf('t') + 2));
                translateVec = parseVector3f(translation);
            }

            // Parse rotation or quaternion
            if (toParse.indexOf('r') != -1)
            {
                String rotation = toParse.substring(toParse.indexOf('r') + 2, toParse.indexOf(']', toParse.indexOf('r') + 2));
                Vector3f rotateVec = parseVector3f(rotation);
                if (rotateVec != null) rotateQuat = TRSRTransformation.quatFromXYZDegrees(rotateVec);
            }
            else if (toParse.indexOf('q') != -1)
            {
                String quaternion = toParse.substring(toParse.indexOf('q') + 2, toParse.indexOf(']', toParse.indexOf('q') + 2));
                Vector4f rotateQuatVec = parseVector4f(quaternion);
                if (rotateQuatVec != null)
                    rotateQuat = new Quat4f(rotateQuatVec.x, rotateQuatVec.y, rotateQuatVec.z, rotateQuatVec.w);
            }

            // Parse scale
            if (toParse.indexOf('s') != -1)
            {
                String scale = toParse.substring(toParse.indexOf('s') + 2, toParse.indexOf(']', toParse.indexOf('s') + 2));
                scaleVec = parseVector3f(scale);
            }

            return new TRSRTransformation(translateVec, rotateQuat, scaleVec, new Quat4f());
        }
        catch (NumberFormatException | StringIndexOutOfBoundsException ex)
        {
            GuidebookMod.logger.warn(String.format("Input TRSRTransformation(t[Xf,Yf,Zf] r[θxf,θyf,θzf] s[Xf,Yf,Zf]) string '%s' cannot be parsed: %s", toParse, ex.getMessage()));
            return null;
        }
    }
}
