import 'package:flutter/material.dart';
import 'package:syncfusion_flutter_pdfviewer/pdfviewer.dart';

import '../../../core/constants/app_colors.dart';

/// Previsualiza un PDF (comprobante) dentro de la app, desde su URL de red.
class PdfPreviewScreen extends StatelessWidget {
  const PdfPreviewScreen({super.key, required this.url, this.title});
  final String url;
  final String? title;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(title ?? 'Comprobante')),
      body: SfPdfViewer.network(
        url,
        onDocumentLoadFailed: (details) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('No se pudo cargar el PDF: ${details.description}')),
          );
        },
      ),
      backgroundColor: AppColors.surfaceAlt,
    );
  }
}
