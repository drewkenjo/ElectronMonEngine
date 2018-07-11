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
  - class: org.jlab.clas12.mon.pid.NumberOfElectrons
    name: NumberOfElectrons
  - class: org.jlab.clas12.mon.pid.NumberOfProtons
    name: NumberOfProtons
  - class: org.jlab.clas12.mon.dc.DCmonitoring
    name: DCmonitoring
  - class: org.jlab.clas12.mon.ftof.FTOFmonitoring
    name: FTOFmonitoring
  - class: org.jlab.clas12.mon.forward.PositiveVertex
    name: PositiveVertex
  - class: org.jlab.clas12.mon.forward.NegativeVertex
    name: NegativeVertex
  - class: org.jlab.clas12.mon.forward.NumberOfNegatives
    name: NumberOfNegatives
  - class: org.jlab.clas12.mon.forward.NumberOfNeutrals
    name: NumberOfNeutrals
  - class: org.jlab.clas12.mon.forward.SamplingFraction
    name: SamplingFraction
configuration:
  io-services:
    writer:
      compression: 2
mime-types:
  - binary/data-hipo

```
