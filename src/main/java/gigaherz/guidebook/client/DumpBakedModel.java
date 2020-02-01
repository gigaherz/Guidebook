package gigaherz.guidebook.client;

import com.google.common.collect.Lists;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.Direction;
import net.minecraftforge.client.model.data.EmptyModelData;

import java.io.*;
import java.util.List;
import java.util.Random;

public class DumpBakedModel
{
    private static Direction[] DIRECTIONS = {
            null, Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN
    };

    public static void dumpToOBJ(File file, String name, IBakedModel model)
    {
        try (OutputStream stream = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(stream))
        {
            int lastTex = 0;
            int lastPos = 0;
            int lastNorm = 0;

            Random rand = new Random(1);

            writer.write(String.format("o %s\n", name));

            for (Direction dir : DIRECTIONS)
            {
                writer.write(String.format("g %s\n", dir));

                List<String> faces = Lists.newArrayList();
                for (BakedQuad quad : model.getQuads(null, dir, rand, EmptyModelData.INSTANCE))
                {
                    VertexFormat fmt = DefaultVertexFormats.BLOCK;
                    int[] data = quad.getVertexData();
                    int byteStart = 0;
                    int byteLen = fmt.getSize();
                    List<Integer> indices0 = Lists.newArrayList();
                    List<Integer> indices1 = Lists.newArrayList();
                    List<Integer> indices2 = Lists.newArrayList();
                    List<Integer> indices3 = Lists.newArrayList();
                    boolean hasTex = false;
                    for (VertexFormatElement element : fmt.getElements())
                    {
                        if (element.getUsage() != VertexFormatElement.Usage.PADDING)
                        {
                            String elementValues0 = decodeElement(data, byteStart, element);
                            String elementValues1 = decodeElement(data, byteStart + byteLen, element);
                            String elementValues2 = decodeElement(data, byteStart + byteLen * 2, element);
                            String elementValues3 = decodeElement(data, byteStart + byteLen * 3, element);
                            String prefix;
                            switch (element.getUsage())
                            {
                                case POSITION:
                                    prefix = "v";
                                    indices0.add(++lastPos);
                                    indices1.add(++lastPos);
                                    indices2.add(++lastPos);
                                    indices3.add(++lastPos);
                                    break;
                                case UV:
                                    prefix = "vt";
                                    indices0.add(++lastTex);
                                    indices1.add(++lastTex);
                                    indices2.add(++lastTex);
                                    indices3.add(++lastTex);
                                    hasTex = true;
                                    break;
                                case NORMAL:
                                    prefix = "vn";
                                    indices0.add(++lastNorm);
                                    indices1.add(++lastNorm);
                                    indices2.add(++lastNorm);
                                    indices3.add(++lastNorm);
                                    break;
                                default:
                                    prefix = element.getUsage().getDisplayName().replace(" ", "");
                                    break;
                            }
                            prefix = element.getIndex() > 0 ? (prefix + element.getIndex()) : prefix;

                            writer.write(String.format("%s %s\n", prefix, elementValues0));
                            writer.write(String.format("%s %s\n", prefix, elementValues1));
                            writer.write(String.format("%s %s\n", prefix, elementValues2));
                            writer.write(String.format("%s %s\n", prefix, elementValues3));
                        }
                        byteStart += element.getType().getSize() * element.getElementCount();
                    }

                    writer.write(String.format("f %s %s %s %s\n",
                            formatIndices(indices0, hasTex),
                            formatIndices(indices1, hasTex),
                            formatIndices(indices2, hasTex),
                            formatIndices(indices3, hasTex)
                    ));
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static String formatIndices(List<Integer> indices, boolean hasTex)
    {
        if (indices.size() == 1)
            return String.format("%d", indices.get(0));
        if (indices.size() == 2 && hasTex)
            return String.format("%d/%d", indices.get(0), indices.get(1));
        if (indices.size() == 2)
            return String.format("%d//%d", indices.get(0), indices.get(1));
        if (indices.size() >= 3)
            return String.format("%d/%d/%d", indices.get(0), indices.get(1), indices.get(2));
        return "1";
    }

    private static String decodeElement(int[] data, int byteStart, VertexFormatElement element)
    {
        switch (element.getType())
        {
            case FLOAT:
                switch (element.getElementCount())
                {
                    case 1:
                        return String.format("%f", getFloatAt(data, byteStart));
                    case 2:
                        return String.format("%f %f", getFloatAt(data, byteStart), getFloatAt(data, byteStart + 4));
                    case 3:
                        return String.format("%f %f %f", getFloatAt(data, byteStart), getFloatAt(data, byteStart + 4), getFloatAt(data, byteStart + 8));
                    case 4:
                        return String.format("%f %f %f %f", getFloatAt(data, byteStart), getFloatAt(data, byteStart + 4), getFloatAt(data, byteStart + 8), getFloatAt(data, byteStart + 12));
                }
                break;
            case UBYTE:
                switch (element.getElementCount())
                {
                    case 1:
                        return String.format("%f", getUByteAt(data, byteStart) / 255.0f);
                    case 2:
                        return String.format("%f %f", getUByteAt(data, byteStart) / 255.0f, getUByteAt(data, byteStart + 1) / 255.0f);
                    case 3:
                        return String.format("%f %f %f", getUByteAt(data, byteStart) / 255.0f, getUByteAt(data, byteStart + 1) / 255.0f, getUByteAt(data, byteStart + 2) / 255.0f);
                    case 4:
                        return String.format("%f %f %f %f", getUByteAt(data, byteStart) / 255.0f, getUByteAt(data, byteStart + 1) / 255.0f, getUByteAt(data, byteStart + 2) / 255.0f, getUByteAt(data, byteStart + 3) / 255.0f);
                }
                break;
            case BYTE:
                switch (element.getElementCount())
                {
                    case 1:
                        return String.format("%f", getByteAt(data, byteStart) / 127.0f);
                    case 2:
                        return String.format("%f %f", getByteAt(data, byteStart) / 127.0f, getByteAt(data, byteStart + 1) / 127.0f);
                    case 3:
                        return String.format("%f %f %f", getByteAt(data, byteStart) / 127.0f, getByteAt(data, byteStart + 1) / 127.0f, getByteAt(data, byteStart + 2) / 127.0f);
                    case 4:
                        return String.format("%f %f %f %f", getByteAt(data, byteStart) / 127.0f, getByteAt(data, byteStart + 1) / 127.0f, getByteAt(data, byteStart + 2) / 127.0f, getByteAt(data, byteStart + 3) / 127.0f);
                }
                break;
            case USHORT:
                switch (element.getElementCount())
                {
                    case 1:
                        return String.format("%d", getUShortAt(data, byteStart));
                    case 2:
                        return String.format("%d %d", getUShortAt(data, byteStart), getUShortAt(data, byteStart + 2));
                    case 3:
                        return String.format("%d %d %d", getUShortAt(data, byteStart), getUShortAt(data, byteStart + 2), getUShortAt(data, byteStart + 4));
                    case 4:
                        return String.format("%d %d %d %d", getUShortAt(data, byteStart), getUShortAt(data, byteStart + 2), getUShortAt(data, byteStart + 4), getUShortAt(data, byteStart + 6));
                }
                break;
            case SHORT:
                switch (element.getElementCount())
                {
                    case 1:
                        return String.format("%d", getShortAt(data, byteStart));
                    case 2:
                        return String.format("%d %d", getShortAt(data, byteStart), getShortAt(data, byteStart + 2));
                    case 3:
                        return String.format("%d %d %d", getShortAt(data, byteStart), getShortAt(data, byteStart + 2), getShortAt(data, byteStart + 4));
                    case 4:
                        return String.format("%d %d %d %d", getShortAt(data, byteStart), getShortAt(data, byteStart + 2), getShortAt(data, byteStart + 4), getShortAt(data, byteStart + 6));
                }
                break;
            case UINT:
                switch (element.getElementCount())
                {
                    case 1:
                        return String.format("%d", getUIntAt(data, byteStart));
                    case 2:
                        return String.format("%d %d", getUIntAt(data, byteStart), getUIntAt(data, byteStart + 4));
                    case 3:
                        return String.format("%d %d %d", getUIntAt(data, byteStart), getUIntAt(data, byteStart + 4), getUIntAt(data, byteStart + 8));
                    case 4:
                        return String.format("%d %d %d %d", getUIntAt(data, byteStart), getUIntAt(data, byteStart + 4), getUIntAt(data, byteStart + 8), getUIntAt(data, byteStart + 12));
                }
                break;
            case INT:
                switch (element.getElementCount())
                {
                    case 1:
                        return String.format("%d", getIntAt(data, byteStart));
                    case 2:
                        return String.format("%d %d", getIntAt(data, byteStart), getIntAt(data, byteStart + 4));
                    case 3:
                        return String.format("%d %d %d", getIntAt(data, byteStart), getIntAt(data, byteStart + 4), getIntAt(data, byteStart + 8));
                    case 4:
                        return String.format("%d %d %d %d", getIntAt(data, byteStart), getIntAt(data, byteStart + 4), getIntAt(data, byteStart + 8), getIntAt(data, byteStart + 12));
                }
                break;
        }
        return "0";
    }

    private static float getFloatAt(int[] data, int n)
    {
        return Float.intBitsToFloat(getIntAt(data, n));
    }

    private static long getUIntAt(int[] data, int n)
    {
        return (getUByteAt(data, n + 3) << 24) + (getUByteAt(data, n + 2) << 16) + (getUByteAt(data, n + 1) << 8) + getUByteAt(data, n);
    }

    private static int getIntAt(int[] data, int n)
    {
        return (getByteAt(data, n + 3) << 24) + (getUByteAt(data, n + 2) << 16) + (getUByteAt(data, n + 1) << 8) + getUByteAt(data, n);
    }

    private static int getUShortAt(int[] data, int n)
    {
        return (getUByteAt(data, n + 1) << 8) + getUByteAt(data, n);
    }

    private static int getShortAt(int[] data, int n)
    {
        return (getByteAt(data, n + 1) << 8) + getUByteAt(data, n);
    }

    private static int getUByteAt(int[] data, int n)
    {
        int idx = n / 4;
        int off = (n % 4) * 8;
        return (data[idx] >> off) & 0xFF;
    }

    private static int getByteAt(int[] data, int n)
    {
        int idx = n / 4;
        int off = 24 - (n % 4) * 8;
        return (data[idx] << off) >> 24;
    }
}
