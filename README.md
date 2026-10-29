# ΔPDF
Automated PDF form filler with Δs.

## Usage
Given a PDF Acrobat Form, a TSV file with rows (Δs) and headers, and
an optional TEMPLATE, it will fill out PDFs according to the template,
for every Δ. The browser version includes a template creator.

TODO: Template Spec

### Using it Online
Link here: [ΔPDF](https://nnoodle.github.io/delta-pdf/)

### CLI Invocation
```shell
$ ./output/delta-pdf.js FORM.pdf DELTAS.tsv TEMPLATE.edn
```

### Building

```shell
$ shadow-cljs release node    # for the CLI utility
$ shadow-cljs release browser # for the browser UI
```
