### Clara YAML

```
io-services:
  reader:
    class: org.jlab.clas.std.services.convertors.HipoToHipoReader
    name: HipoToHipoReader
  writer:
    class: org.jlab.clas.std.services.convertors.HipoToHipoWriter
    name: HipoToHipoWriter
services:
  - class: org.jlab.clas12.mon.NumberOfElectrons
    name: NumberOfElectrons
configuration:
  io-services:
    writer:
      compression: 2
mime-types:
  - binary/data-hipo

```
