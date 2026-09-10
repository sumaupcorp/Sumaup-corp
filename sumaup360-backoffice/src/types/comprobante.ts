/**
 * Tipos del flujo de comprobantes (función núcleo del backoffice).
 *
 * SOLO tipos/interfaces/enums (contrato). Sin lógica de negocio ni imports.
 * El BACKEND es la autoridad del estado; estos tipos describen la forma de los
 * datos que el frontend RECIBE y REFLEJA. A futuro deberían generarse desde
 * el OpenAPI del backend.
 *
 * IMPORTANTE: el comprobante lo sube un USUARIO DE LA APP (Línea Personas) y lo
 * procesa un CONTADOR (staff). No confundir esas identidades.
 *
 * Fuente: ../../docs/COMPROBANTE_INTAKE.md y skill backoffice-comprobante-flow.
 */

/** Estado del comprobante en su ciclo de vida. */
export type ComprobanteStatus =
  | "pendiente" // recién subido por el usuario; en cola
  | "en_proceso" // un contador lo tomó / lo está cargando en SUMAUP360
  | "procesado" // cargado y conforme; ciclo cerrado
  | "observado"; // requiere corrección del usuario (lleva nota)

/** Tipo de documento tributario. */
export type ComprobanteTipo =
  | "boleta"
  | "factura"
  | "rxh"; // recibo por honorarios

/** Comprobante subido desde la app móvil y gestionado en el backoffice. */
export interface Comprobante {
  id: string;
  /** Usuario de la app (Línea Personas) que lo subió. NO es staff. */
  appUserId: string;
  tipo: ComprobanteTipo;
  /** Monto en soles (PEN). */
  montoSoles: number;
  /** Fecha del comprobante, ISO 8601. */
  fecha: string;
  /** URL del archivo (boleta/factura escaneada) servida por el backend. */
  archivoUrl: string;
  status: ComprobanteStatus;
  /** Contador (staff) al que está asignado, si ya fue tomado. */
  asignadoAContadorId?: string;
  /** Nota del contador; obligatoria de hecho cuando el estado es "observado". */
  nota?: string;
  /** ISO 8601: cuándo lo recibió el backend. */
  createdAt?: string;
  /** ISO 8601: último cambio de estado. */
  updatedAt?: string;
}

/**
 * Ítem de la cola de intake que ve el contador en el backoffice.
 * Versión liviana del comprobante para listar/priorizar.
 */
export interface IntakeQueueItem {
  comprobanteId: string;
  appUserId: string;
  tipo: ComprobanteTipo;
  montoSoles: number;
  status: ComprobanteStatus;
  /** ISO 8601: cuándo entró a la cola (para ordenar por antigüedad). */
  recibidoEn: string;
  /** Contador asignado, si ya fue tomado. */
  asignadoAContadorId?: string;
}
