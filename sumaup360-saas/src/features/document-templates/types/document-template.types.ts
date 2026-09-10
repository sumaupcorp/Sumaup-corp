// Tipos del modulo de plantillas de documentos (SaaS dashboard).
// Reflejan el contrato del backend (com.sumaup360.erp.documenttemplate). El backend es la
// autoridad: el frontend solo configura y previsualiza; el PDF oficial lo genera el backend.

export type DocumentType =
  | "SALE_RECEIPT"
  | "INVOICE"
  | "INTERNAL_SALE_NOTE"
  | "DELIVERY_GUIDE"
  | "POS_TICKET"
  | "KITCHEN_ORDER"
  | "PRE_BILL"
  | "QUOTATION"
  | "CREDIT_NOTE"
  | "DEBIT_NOTE";

export type PrintFormat = "A4" | "THERMAL_80MM" | "THERMAL_58MM";

export type DocumentStatus =
  | "DRAFT"
  | "ISSUED"
  | "CANCELLED"
  | "VOIDED"
  | "SENT"
  | "ACCEPTED"
  | "REJECTED";

export const DOCUMENT_TYPE_LABELS: Record<DocumentType, string> = {
  SALE_RECEIPT: "Boleta de venta",
  INVOICE: "Factura",
  INTERNAL_SALE_NOTE: "Nota de venta",
  DELIVERY_GUIDE: "Guia de remision",
  POS_TICKET: "Ticket POS",
  KITCHEN_ORDER: "Comanda",
  PRE_BILL: "Precuenta",
  QUOTATION: "Cotizacion",
  CREDIT_NOTE: "Nota de credito",
  DEBIT_NOTE: "Nota de debito",
};

export const PRINT_FORMAT_LABELS: Record<PrintFormat, string> = {
  A4: "A4",
  THERMAL_80MM: "Ticket termico 80mm",
  THERMAL_58MM: "Ticket termico 58mm",
};

export interface DocumentTemplate {
  id: string;
  companyId: string;
  branchId?: string | null;
  businessTypeId?: string | null;
  documentType: DocumentType;
  printFormat: PrintFormat;
  templateName: string;
  templateCode: string;
  isDefault: boolean;
  active: boolean;
  primaryColor: string;
  secondaryColor: string;
  fontFamily: string;
  logoUrl?: string | null;
  showLogo: boolean;
  showQr: boolean;
  showPaymentInfo: boolean;
  showSeller: boolean;
  showCustomerAddress: boolean;
  showBusinessExtraFields: boolean;
  headerConfig?: string | null;
  bodyConfig?: string | null;
  footerConfig?: string | null;
  customCss?: string | null;
}

export interface DocumentTemplateRequest {
  companyId: string;
  branchId?: string | null;
  businessTypeId?: string | null;
  documentType: DocumentType;
  printFormat: PrintFormat;
  templateName: string;
  templateCode: string;
  primaryColor?: string;
  secondaryColor?: string;
  fontFamily?: string;
  logoUrl?: string | null;
  showLogo?: boolean;
  showQr?: boolean;
  showPaymentInfo?: boolean;
  showSeller?: boolean;
  showCustomerAddress?: boolean;
  showBusinessExtraFields?: boolean;
  headerConfig?: string | null;
  bodyConfig?: string | null;
  footerConfig?: string | null;
  customCss?: string | null;
}

/** Configuracion sin guardar del editor: el backend la pisa sobre la resuelta. */
export interface InlinePreviewConfig {
  primaryColor?: string | null;
  secondaryColor?: string | null;
  logoUrl?: string | null;
  showLogo?: boolean | null;
  showQr?: boolean | null;
  showPaymentInfo?: boolean | null;
  showSeller?: boolean | null;
  showCustomerAddress?: boolean | null;
  showBusinessExtraFields?: boolean | null;
  footerText?: string | null;
  legalMessage?: string | null;
  commercialMessage?: string | null;
  thankYouMessage?: string | null;
}

export interface DocumentPreviewRequest {
  templateId?: string | null;
  companyId?: string | null;
  documentType?: DocumentType;
  printFormat?: PrintFormat;
  businessType?: string | null; // restaurant | hardware | pharmacy | delivery
  config?: InlinePreviewConfig | null;
}

/** Mensajes del pie del documento (se guardan como JSON en footerConfig). */
export interface FooterMessages {
  footerText: string;
  legalMessage: string;
  commercialMessage: string;
  thankYouMessage: string;
}

export function parseFooterConfig(json?: string | null): FooterMessages {
  const empty: FooterMessages = { footerText: "", legalMessage: "", commercialMessage: "", thankYouMessage: "" };
  if (!json) return empty;
  try {
    const v = JSON.parse(json) as Partial<FooterMessages>;
    return {
      footerText: v.footerText ?? "",
      legalMessage: v.legalMessage ?? "",
      commercialMessage: v.commercialMessage ?? "",
      thankYouMessage: v.thankYouMessage ?? "",
    };
  } catch {
    return empty;
  }
}

export function serializeFooterConfig(f: FooterMessages): string | null {
  const clean = Object.fromEntries(
    Object.entries(f).filter(([, v]) => typeof v === "string" && v.trim() !== ""));
  return Object.keys(clean).length > 0 ? JSON.stringify(clean) : null;
}

export interface DocumentSeries {
  id: string;
  companyId: string;
  branchId?: string | null;
  documentType: DocumentType;
  series: string;
  currentNumber: number;
  active: boolean;
}

export interface CreateSeriesRequest {
  companyId: string;
  branchId?: string | null;
  documentType: DocumentType;
  series: string;
  active?: boolean;
}
