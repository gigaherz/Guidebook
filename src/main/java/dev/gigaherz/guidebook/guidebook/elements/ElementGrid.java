package dev.gigaherz.guidebook.guidebook.elements;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import dev.gigaherz.guidebook.guidebook.BookDocument;
import dev.gigaherz.guidebook.guidebook.BookParsingException;
import dev.gigaherz.guidebook.guidebook.IBookGraphics;
import dev.gigaherz.guidebook.guidebook.ParsingContext;
import dev.gigaherz.guidebook.guidebook.conditions.ConditionContext;
import dev.gigaherz.guidebook.guidebook.drawing.VisualElement;
import dev.gigaherz.guidebook.guidebook.drawing.VisualPanel;
import dev.gigaherz.guidebook.guidebook.templates.TemplateDefinition;
import dev.gigaherz.guidebook.guidebook.util.Point;
import dev.gigaherz.guidebook.guidebook.util.Rect;
import dev.gigaherz.guidebook.guidebook.util.Size;
import net.minecraft.client.resources.model.Material;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import java.util.*;

public class ElementGrid extends Element
{
    public boolean heightPercent;
    public boolean widthPercent;
    public Integer height;
    public Integer width;

    public List<Row> rows = new ArrayList<>();
    public List<Column> cols = new ArrayList<>();

    @Override
    public void parse(ParsingContext context, NamedNodeMap attributes)
    {
        super.parse(context, attributes);

        Node attr = attributes.getNamedItem("height");
        if (attr != null)
        {
            String t = attr.getTextContent();
            if (t.endsWith("%"))
            {
                heightPercent = true;
                t = t.substring(0, t.length() - 1);
            }

            height = Ints.tryParse(t);
        }

        attr = attributes.getNamedItem("width");
        if (attr != null)
        {
            String t = attr.getTextContent();
            if (t.endsWith("%"))
            {
                widthPercent = true;
                t = t.substring(0, t.length() - 1);
            }

            width = Ints.tryParse(t);
        }
    }

    @Override
    public void parseChildNodes(ParsingContext context, NodeList rowNodes, Map<String, TemplateDefinition> templates, TextStyle defaultStyle)
    {
        int numColumns = -1;

        for (int i = 0; i < rowNodes.getLength(); ++i)
        {
            Node rowNode = rowNodes.item(i);
            String rowNodeName = rowNode.getNodeName();
            if (rowNodeName.equals("row"))
            {
                if (rowNode.hasChildNodes())
                {
                    var row = new Row();
                    var rowColumns = 0;
                    if (rowNode.hasAttributes())
                        row.parse(rowNode.getAttributes());

                    var colNodes = rowNode.getChildNodes();
                    for (int j = 0; j < colNodes.getLength(); ++j)
                    {
                        Node colNode = colNodes.item(j);
                        String colNodeName = colNode.getNodeName();
                        if (colNodeName.equals("col"))
                        {
                            if (numColumns < 0)
                            {
                                var col = new Column();
                                if (colNode.hasAttributes())
                                    col.parse(colNode.getAttributes());
                                cols.add(col);
                            }

                            var cell = new Cell();
                            if (colNode.hasAttributes())
                                cell.parse(colNode.getAttributes());

                            if (colNode.hasChildNodes())
                            {
                                var list = new ArrayList<Element>();

                                BookDocument.parseChildElements(context, colNode.getChildNodes(), list, templates, true, defaultStyle);

                                if (list.size() == 1)
                                    cell.content = list.get(0);
                                else
                                    cell.content = new ElementPanel(list);
                            }

                            row.cells.add(cell);

                            rowColumns+=cell.colspan;
                        }
                        else
                        {
                            if (colNode.getNodeType() == Node.ELEMENT_NODE)
                                throw new BookParsingException("Only 'col' elements are allowed inside a row tag.");
                        }
                    }

                    if (numColumns < 0)
                        numColumns = rowColumns;
                    else if(numColumns != rowColumns)
                        throw new BookParsingException("All rows in a grid must have the same number of columns! " + rowColumns + " != " + numColumns);

                    rows.add(row);
                }
            }
            else
            {
                if (rowNode.getNodeType() == Node.ELEMENT_NODE)
                    throw new BookParsingException("Only 'row' elements are allowed inside a grid tag.");
            }
        }
    }

    @Override
    public String toString(boolean complete)
    {
        // TODO: Complete mode
        return "<grid ...></grid>";
    }

    @Override
    public boolean reevaluateConditions(ConditionContext ctx)
    {
        boolean oldValue = conditionResult;
        conditionResult = condition == null || condition.test(ctx);

        boolean anyChanged = conditionResult != oldValue;
        for(var row : rows)
        {
            for(var cell : row.cells)
            {
                if (cell.content != null)
                    anyChanged |= cell.content.reevaluateConditions(ctx);
            }
        }

        return anyChanged;
    }

    @Override
    public int reflow(List<VisualElement> list, IBookGraphics nav, Rect bounds, Rect pageBounds)
    {
        List<VisualElement> visuals = Lists.newArrayList();

        Point adjustedPosition = applyPosition(bounds.position, bounds.position);
        Rect adjustedBounds = new Rect(adjustedPosition, bounds.size);

        int top = adjustedPosition.y;

        var accHeight = 0;
        for (var row : rows)
        {
            row.computedHeight = adjustedBounds.size.height / rows.size();
            if (row.height != null)
                row.computedHeight = (row.heightPercent ? (row.height * bounds.size.height / 100) : row.height);
            accHeight += row.computedHeight;
        }
        if (accHeight > adjustedBounds.size.height)
        {
            var div = accHeight;
            accHeight = 0;
            for (var row : rows)
            {
                row.computedHeight = row.computedHeight * adjustedBounds.size.height / div;
                accHeight += row.computedHeight;
            }
        }

        var accWidth = 0;
        for (var col : cols)
        {
            col.computedWidth = adjustedBounds.size.width / cols.size();
            if (col.width != null)
                col.computedWidth = (col.widthPercent ? (col.width * bounds.size.width / 100) : col.width);
            accWidth += col.computedWidth;
        }
        if (accWidth > adjustedBounds.size.width)
        {
            var div = accWidth;
            accWidth = 0;
            for (var col : cols)
            {
                col.computedWidth = col.computedWidth * adjustedBounds.size.width / div;
                accWidth += col.computedWidth;
            }
        }

        for (var row : rows)
        {
            var rowHeight = row.computedHeight;

            int col = 0;
            int left = adjustedBounds.position.x;
            for(var cell : row.cells)
            {
                var cellWidth = 0;
                for(int c = 0; c< cell.colspan;c++)
                {
                    cellWidth += cols.get(col).computedWidth;
                }

                var cellBounds = new Rect(left, top, cellWidth, rowHeight);
                if (cell.content != null)
                {
                    cell.content.reflow(visuals, nav, cellBounds, pageBounds);
                }

                left += cellWidth;
            }
            top += rowHeight;
        }

        if (position != POS_RELATIVE)
        {
            top = bounds.position.y;
        }
        else if (height != null)
        {
            top = adjustedPosition.y + (heightPercent ? (height * bounds.size.height / 100) : height);
        }

        if (visuals.size() > 0)
        {
            Size size = new Size(bounds.size.width, top - adjustedPosition.y);

            VisualPanel p = new VisualPanel(size, position, baseline, verticalAlignment);

            p.position = adjustedPosition;

            p.children.addAll(visuals);

            list.add(p);
        }

        return top;
    }

    @Override
    public void findTextures(Set<Material> textures)
    {
        for(var row : rows)
        {
            for(var cell : row.cells)
            {
                if (cell.content != null)
                    cell.content.findTextures(textures);
            }
        }
    }

    @Override
    public Element copy()
    {
        ElementGrid space = super.copy(new ElementGrid());
        space.heightPercent = heightPercent;
        space.height = this.height;
        return space;
    }

    @Nullable
    @Override
    public Element applyTemplate(ParsingContext context, List<Element> sourceElements)
    {
        if (rows.size() == 0 || cols.size() == 0)
            return null;

        ElementGrid grid = super.copy(new ElementGrid());
        grid.height = height;
        grid.heightPercent = heightPercent;
        grid.width = width;
        grid.widthPercent = widthPercent;

        for(var row : rows)
        {
            var row1 = new Row();
            row1.height = row.height;
            row1.heightPercent = row.heightPercent;

            for(var cell : row.cells)
            {
                var cell1 = new Cell();
                var t = cell.content.applyTemplate(context, sourceElements);
                if (t != null)
                {
                    cell1.content = t;
                }
                row1.cells.add(cell1);
            }

            if (row1.cells.stream().anyMatch(c -> c.content != null))
                grid.rows.add(row1);
        }

        for(var col : cols)
        {
            var col1 = new Column();
            col1.width = col.width;
            col1.widthPercent = col.widthPercent;
            grid.cols.add(col1);
        }

        if (rows.size() == 0 || cols.size() == 0)
            return null;

        return grid;
    }

    @Override
    public boolean supportsPageLevel()
    {
        return true;
    }

    private static class Column
    {
        public boolean widthPercent;
        public Integer width;
        public int computedWidth;

        public void parse(NamedNodeMap attributes)
        {
            var attr = attributes.getNamedItem("width");
            if (attr != null)
            {
                String t = attr.getTextContent();
                if (t.endsWith("%"))
                {
                    widthPercent = true;
                    t = t.substring(0, t.length() - 1);
                }

                width = Ints.tryParse(t);
            }
        }
    }

    private static class Cell
    {
        public Integer colspan = 1;
        @Nullable
        public Element content;

        public void parse(NamedNodeMap attributes)
        {
            Node attr = attributes.getNamedItem("colspan");
            if (attr != null)
            {
                String t = attr.getTextContent();
                colspan = Ints.tryParse(t);
            }
        }
    }

    private static class Row
    {
        public boolean heightPercent;
        public Integer height;
        public List<Cell> cells = new ArrayList<>();
        public int computedHeight;

        public void parse(NamedNodeMap attributes)
        {
            Node attr = attributes.getNamedItem("height");
            if (attr != null)
            {
                String t = attr.getTextContent();
                if (t.endsWith("%"))
                {
                    heightPercent = true;
                    t = t.substring(0, t.length() - 1);
                }

                height = Ints.tryParse(t);
            }
        }
    }
}
