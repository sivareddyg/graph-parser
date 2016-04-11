package uk.ac.ed.ilcc.others;

import java.awt.Dimension;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.googlecode.whatswrong.NLPCanvasRenderer;
import com.googlecode.whatswrong.NLPInstance;
import com.googlecode.whatswrong.SingleSentenceRenderer;
import com.googlecode.whatswrong.Token;

public class RenderSVG {
  private double zoomInFactor = 1.5;
  private static NLPCanvasRenderer renderer = new SingleSentenceRenderer();

  public RenderSVG() {}

  private String drawSVGTree(List<JsonObject> words) throws IOException {
    NLPInstance instance = new NLPInstance();
    instance.addToken().addProperty(SentenceKeys.WORD_KEY, "[root]")
        .addProperty(SentenceKeys.INDEX_KEY, "0");

    int sentStart = 1;
    if (words.size() > 0 && words.get(0).has(SentenceKeys.INDEX_KEY))
      sentStart = words.get(0).get(SentenceKeys.INDEX_KEY).getAsInt();

    for (JsonObject word : words) {
      Token token = instance.addToken();
      token.addProperty(SentenceKeys.WORD_KEY, word.get(SentenceKeys.WORD_KEY)
          .getAsString());

      if (word.has(SentenceKeys.POS_KEY)) {
        token.addProperty(SentenceKeys.POS_KEY, word.get(SentenceKeys.POS_KEY)
            .getAsString());
      }

      if (word.has(SentenceKeys.NER_KEY)) {
        token.addProperty(SentenceKeys.NER_KEY, word.get(SentenceKeys.NER_KEY)
            .getAsString());
      }

      if (word.has(SentenceKeys.INDEX_KEY)) {
        token.addProperty(SentenceKeys.INDEX_KEY,
            word.get(SentenceKeys.INDEX_KEY).getAsString());
      }
    }

    for (JsonObject word : words) {
      if (word.has(SentenceKeys.HEAD_KEY)
          && word.has(SentenceKeys.DEPENDENCY_KEY)) {
        int head = word.get(SentenceKeys.HEAD_KEY).getAsInt();
        if (head != 0)
          head = head - sentStart + 1;
        int current =
            word.get(SentenceKeys.INDEX_KEY).getAsInt() - sentStart + 1;
        instance.addDependency(head, current,
            word.get(SentenceKeys.DEPENDENCY_KEY).getAsString(),
            SentenceKeys.DEPENDENCY_KEY);
      }
    }

    // Get a DOMImplementation.
    DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
    // Create an instance of org.w3c.dom.Document.
    String svgNS = "http://www.w3.org/2000/svg";
    Document document = domImpl.createDocument(svgNS, "svg", null);

    // Create an instance of the SVG Generator.
    SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

    Dimension canvasSize = renderer.render(instance, svgGenerator);

    Element root = svgGenerator.getRoot();
    root.setAttributeNS(null, "viewBox", String.format("0 0 %d %d",
        canvasSize.width + 2, canvasSize.height + 10));

    root.setAttributeNS(null, "height",
        new Long(Math.round(getZoomInFactor() * canvasSize.height)).toString());
    root.setAttributeNS(null, "width",
        new Long(Math.round(getZoomInFactor() * canvasSize.width)).toString());

    root.setAttributeNS(null, "preserveAspectRatio", "xMinYMin meet");

    try {
      TransformerFactory transFactory = TransformerFactory.newInstance();
      Transformer transformer = transFactory.newTransformer();
      StringWriter buffer = new StringWriter();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

      transformer.transform(new DOMSource(root), new StreamResult(buffer));
      String str = buffer.toString();
      return str;
    } catch (TransformerException e) {
      e.printStackTrace();
    }

    // Alternative Writer
    // ByteArrayOutputStream stream = new ByteArrayOutputStream();
    // BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(stream));
    // svgGenerator.stream(root, bw);
    // bw.close();
    // return stream.toString();
    return null;
  }

  public String drawSVGTrees(JsonObject jsonSentence) throws IOException {
    if (!jsonSentence.has(SentenceKeys.WORDS_KEY))
      return null;
    StringBuilder sb = new StringBuilder();
    sb.append("<div>\n");
    List<JsonObject> singleSentence = new ArrayList<>();
    JsonArray words = jsonSentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray();
    int currentWordIndex = -1;
    for (JsonElement word : words) {
      currentWordIndex++;
      JsonObject wordObject = word.getAsJsonObject();
      singleSentence.add(wordObject);
      if ((wordObject.has(SentenceKeys.SENT_END) && wordObject.get(
          SentenceKeys.SENT_END).getAsBoolean())
          || currentWordIndex + 1 == words.size()) {
        sb.append(drawSVGTree(singleSentence));
        sb.append("\n<br>\n");
        singleSentence = new ArrayList<>();
      }
    }
    sb.append("</div>");
    return sb.toString();
  }

  public double getZoomInFactor() {
    return zoomInFactor;
  }

  public void setZoomInFactor(double zoomInFactor) {
    this.zoomInFactor = zoomInFactor;
  }
}
