package com.sumaup360.erp.documenttemplate.renderer;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sumaup360.common.error.BadRequestException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

/**
 * Genera el PDF (fuente OFICIAL) desde HTML, en el backend. Usa Jsoup para normalizar el HTML
 * a XHTML bien formado y OpenHTMLToPDF para producir el PDF.
 *
 * Las imagenes remotas (p. ej. el logo subido a Firebase Storage) se descargan y se
 * incrustan como data URI antes del render: el PDF sale igual aunque el visor no tenga
 * red, y el render no depende del resolvedor remoto de OpenHTMLToPDF.
 */
@Service
public class DocumentPdfService {

    private static final int MAX_IMAGE_BYTES = 2 * 1024 * 1024; // 2MB por imagen

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public byte[] htmlToPdf(String html) {
        try {
            Document jsoupDoc = Jsoup.parse(html);
            inlineRemoteImages(jsoupDoc);
            jsoupDoc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
            String xhtml = jsoupDoc.html();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(xhtml, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BadRequestException("No se pudo generar el PDF: " + e.getMessage());
        }
    }

    /** Reemplaza <img src="http(s)..."> por data URIs; si una imagen falla, se quita. */
    private void inlineRemoteImages(Document doc) {
        for (Element img : doc.select("img[src^=http]")) {
            String src = img.attr("src");
            try {
                HttpResponse<byte[]> res = httpClient.send(
                        HttpRequest.newBuilder(URI.create(src))
                                .timeout(Duration.ofSeconds(8)).GET().build(),
                        HttpResponse.BodyHandlers.ofByteArray());
                String contentType = res.headers().firstValue("content-type").orElse("");
                if (res.statusCode() == 200 && contentType.startsWith("image/")
                        && res.body().length > 0 && res.body().length <= MAX_IMAGE_BYTES) {
                    img.attr("src", "data:" + contentType + ";base64,"
                            + Base64.getEncoder().encodeToString(res.body()));
                } else {
                    img.remove();
                }
            } catch (Exception e) {
                img.remove(); // sin logo antes que sin PDF
            }
        }
    }
}
