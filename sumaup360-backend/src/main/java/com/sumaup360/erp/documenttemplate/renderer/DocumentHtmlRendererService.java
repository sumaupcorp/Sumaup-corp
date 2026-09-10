package com.sumaup360.erp.documenttemplate.renderer;

import com.sumaup360.erp.documenttemplate.dto.DocumentData;
import com.sumaup360.erp.documenttemplate.dto.TemplateConfig;
import com.sumaup360.erp.documenttemplate.enums.PrintFormat;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Renderiza el HTML del documento con Thymeleaf. UNA plantilla base por formato; el contenido
 * y los campos por rubro se controlan con los datos y la configuracion (no hay plantilla por rubro).
 */
@Service
public class DocumentHtmlRendererService {

    private final TemplateEngine templateEngine;

    public DocumentHtmlRendererService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String renderHtml(PrintFormat format, DocumentData data, TemplateConfig config) {
        Context ctx = new Context();
        ctx.setVariable("doc", data);
        ctx.setVariable("cfg", config);
        return templateEngine.process(format.getTemplateName(), ctx);
    }
}
