package eu.europeana.processing.harvesting.processor;

import eu.europeana.metis.transformation.service.EuropeanaIdException;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordKey;
import eu.europeana.processing.model.ExecutionRecordResult;
import org.apache.flink.streaming.api.functions.ProcessFunction.Context;
import org.apache.flink.util.Collector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdAssigningOperatorTest {

  @Mock
  private Collector<ExecutionRecordResult> mockOut;

  @Mock
  private Context mockContext;

  @Test
  void shouldCorrectlyGenerateEuropeanaIdentifier() throws EuropeanaIdException {
    //given
    IdAssigningOperator idAssigningOperator = new IdAssigningOperator();

    ExecutionRecordResult input = ExecutionRecordResult
        .builder()
        .executionRecord(ExecutionRecord
            .builder()
            .executionRecordKey(ExecutionRecordKey
                .builder()
                .datasetId("example-dataset-ID")
                .executionId("example-execution-ID")
                .recordId("example-record-ID")
                .build())
            .executionName("Example-execution")
            .recordData(TESTED_RECORD)
            .build())
        .build();

    //when
    idAssigningOperator.processElement(input, mockContext, mockOut);

    //then
    ArgumentCaptor<ExecutionRecordResult> captor = ArgumentCaptor.forClass(ExecutionRecordResult.class);
    Mockito.verify(mockOut, Mockito.times(1)).collect(captor.capture());

    ExecutionRecordResult emitted = captor.getValue();

    Assertions.assertNotNull(emitted);
    Assertions.assertEquals("/example-dataset-I/object_NLS2__RS_643______06VMZI9", emitted.getRecordId());
    Assertions.assertEquals(TESTED_RECORD, emitted.getRecordData());
    Assertions.assertNull(emitted.getException());
    Assertions.assertEquals("example-execution-ID", emitted.getExecutionRecord().getExecutionRecordKey().getExecutionId());
    Assertions.assertEquals("/example-dataset-I/object_NLS2__RS_643______06VMZI9", emitted.getExecutionRecord().getExecutionRecordKey().getRecordId());
    Assertions.assertEquals("example-dataset-ID", emitted.getExecutionRecord().getExecutionRecordKey().getDatasetId());
  }

  @Test
  void shouldThrowExceptionForEmptyRecord() {
    //given
    IdAssigningOperator idAssigningOperator = new IdAssigningOperator();

    ExecutionRecordResult input = ExecutionRecordResult
        .builder()
        .executionRecord(ExecutionRecord
            .builder()
            .executionRecordKey(ExecutionRecordKey
                .builder()
                .datasetId("example-dataset-ID")
                .executionId("example-execution-ID")
                .recordId("example-record-ID")
                .build())
            .executionName("Example-execution")
            .recordData(EMPTY_RECORD)
            .build())
        .build();

    //then
    Assertions.assertThrows(EuropeanaIdException.class,
        () -> idAssigningOperator.processElement(input, mockContext, mockOut)
    );
  }

  @Test
  void shouldThrowExceptionForRecordWithoutRdfAbout() {
    //given
    IdAssigningOperator idAssigningOperator = new IdAssigningOperator();

    ExecutionRecordResult input = ExecutionRecordResult
        .builder()
        .executionRecord(ExecutionRecord
            .builder()
            .executionRecordKey(ExecutionRecordKey
                .builder()
                .datasetId("example-dataset-ID")
                .executionId("example-execution-ID")
                .recordId("example-record-ID")
                .build())
            .executionName("Example-execution")
            .recordData(TESTED_RECORD_WITHOUT_RDF_ABOUT)
            .build())
        .build();

    //then
    Assertions.assertThrows(EuropeanaIdException.class,
        () -> idAssigningOperator.processElement(input, mockContext, mockOut)
    );
  }

  private static final String EMPTY_RECORD =
      """
          """;

  private static final String TESTED_RECORD =
      """
          <?xml version="1.0" encoding="UTF-8"?>
             <rdf:RDF xmlns="http://www.openarchives.org/OAI/2.0/"
                      xmlns:cc="http://creativecommons.org/ns#"
                      xmlns:crm="http://www.cidoc-crm.org/cidoc-crm/"
                      xmlns:dc="http://purl.org/dc/elements/1.1/"
                      xmlns:dcterms="http://purl.org/dc/terms/"
                      xmlns:doap="http://usefulinc.com/ns/doap#"
                      xmlns:edm="http://www.europeana.eu/schemas/edm/"
                      xmlns:foaf="http://xmlns.com/foaf/0.1/"
                      xmlns:ore="http://www.openarchives.org/ore/terms/"
                      xmlns:owl="http://www.w3.org/2002/07/owl#"
                      xmlns:rdau="http://www.rdaregistry.info/Elements/u/"
                      xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                      xmlns:skos="http://www.w3.org/2004/02/skos/core#"
                      xmlns:svcs="http://rdfs.org/sioc/services#"
                      xmlns:wgs84_pos="http://www.w3.org/2003/01/geo/wgs84_pos#"
                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <edm:ProvidedCHO rdf:about="http://www.manuscriptorium.com/object/NLS2__RS_643______06VMZI9">
                 <dc:date>1301-1320</dc:date>
                 <dc:description>Четворојеванђеље (без Теофилактових предговора) и праксапостол, са синаксарима, месецословима и распоредима читања у разним приликама.</dc:description>
                 <dc:description>Са опсецањем маргина одсечене су и ознаке свешчица, осим 19. и 10.</dc:description>
                 <dc:description>Недостаје 1 л. на почетку, 1 л. иза л. 3, 1 л. иза л. 4, цела св. (8 л.) иза л. 14, 1 л. иза л. 29, 1 л. иза л. 165 и 1 или 2 л. на крају. На л. 2 одсечена је десна страна, на л. 3 и 4 доња маргина, док су од л. 23-25 и 29 остали само одерци.</dc:description>
                 <dc:description>Уставно писмо; два писара.</dc:description>
                 <dc:description>Заставица од кругова са вегетабилним изданцима који образују крст.</dc:description>
                 <dc:description>Киноварни иницијали.</dc:description>
                 <dc:description>Више записа на л. 1r: о упокојењу презвитера Витомира, затим, помен неког Михаила Бугарина, као и вежба руке.</dc:description>
                 <dc:description>Крст на Голготи у централном пољу предње корице.</dc:description>
                 <dc:description>дрво, кожа 27,8215</dc:description>
                 <dc:description>Недостају споне са копчама.</dc:description>
                 <dc:identifier>Рс 643 </dc:identifier>
                 <dc:language>cu</dc:language>
                 <dc:title>Четворојеванђеље и праксапостол</dc:title>
                 <dc:format xml:lang="en">codex</dc:format>
                 <dcterms:created>1301-1320</dcterms:created>
                 <dcterms:created xml:lang="sr">почетак XIV в.</dcterms:created>
                 <dcterms:extent>21,2 x 28,2</dcterms:extent>
                 <dcterms:isReferencedBy>Љ. Стојановић, Каталог Народне библиотеке у Београду. IV Рукописи и старе штампане књиге, Београд 1903, бр. 92, 31-32; Љ. Штављанин-Ђорђевић, М. Гроздановић-Пајић, Опис ћирилских рукописа Народне библиотеке Србије. Књига прва, Београд 1986, 329-331. </dcterms:isReferencedBy>
                 <dcterms:medium xml:lang="en">parchment</dcterms:medium>
                 <dcterms:spatial xml:lang="sr">Србија</dcterms:spatial>
                 <edm:currentLocation>Belgrade</edm:currentLocation>
                 <edm:type>TEXT</edm:type>
              </edm:ProvidedCHO>
              <edm:WebResource rdf:about="https://imagines.manuscriptorium.com/loris/NLS___-NLS2__RS_643______06VMZI9-sr/id_001/full/full/0/default.jpg">
                 <dc:format>image/jpg</dc:format>
                 <dc:type>text</dc:type>
                 <dc:type>images</dc:type>
                 <dcterms:conformsTo>TEI P5 ENRICH Schema</dcterms:conformsTo>
                 <edm:rights rdf:resource="http://creativecommons.org/licenses/by-nc-sa/4.0/"/>
                 <dcterms:isReferencedBy rdf:resource="https://collectiones.manuscriptorium.com/assorted/NLS___/NLS2__/9/NLS___-NLS2__RS_643______06VMZI9-sr"/>
                 <svcs:has_service rdf:resource="https://imagines.manuscriptorium.com/loris/NLS___-NLS2__RS_643______06VMZI9-sr/id_001/"/>
              </edm:WebResource>
              <ore:Aggregation rdf:about="http://www.manuscriptorium.com/apps/index.php?direct=record&amp;pid=NLS___-NLS2__RS_643______06VMZI9-sr">
                 <edm:aggregatedCHO rdf:resource="http://www.manuscriptorium.com/object/NLS2__RS_643______06VMZI9"/>
                 <edm:dataProvider>National library of Serbia</edm:dataProvider>
                 <edm:isShownAt rdf:resource="http://www.manuscriptorium.com/apps/index.php?direct=record&amp;pid=NLS___-NLS2__RS_643______06VMZI9-sr"/>
                 <edm:isShownBy rdf:resource="https://imagines.manuscriptorium.com/loris/NLS___-NLS2__RS_643______06VMZI9-sr/id_001/full/full/0/default.jpg"/>
                 <edm:object rdf:resource="https://imagines.manuscriptorium.com/loris/NLS___-NLS2__RS_643______06VMZI9-sr/id_001/full/!400,400/0/default.jpg"/>
                 <edm:provider>Manuscriptorium - National Library of the Czech Republic</edm:provider>
                 <edm:rights rdf:resource="http://creativecommons.org/licenses/by-nc-sa/4.0/"/>
              </ore:Aggregation>
              <svcs:Service rdf:about="https://imagines.manuscriptorium.com/loris/NLS___-NLS2__RS_643______06VMZI9-sr/id_001/">
                 <dcterms:conformsTo rdf:resource="http://iiif.io/api/image"/>
                 <doap:implements rdf:resource="http://iiif.io/api/image/2/level2.json"/>
              </svcs:Service>
          </rdf:RDF>
          """;

  private static final String TESTED_RECORD_WITHOUT_RDF_ABOUT =
      """
          <?xml version="1.0" encoding="UTF-8"?>
             <rdf:RDF xmlns="http://www.openarchives.org/OAI/2.0/"
                      xmlns:cc="http://creativecommons.org/ns#"
                      xmlns:crm="http://www.cidoc-crm.org/cidoc-crm/"
                      xmlns:dc="http://purl.org/dc/elements/1.1/"
                      xmlns:dcterms="http://purl.org/dc/terms/"
                      xmlns:doap="http://usefulinc.com/ns/doap#"
                      xmlns:edm="http://www.europeana.eu/schemas/edm/"
                      xmlns:foaf="http://xmlns.com/foaf/0.1/"
                      xmlns:ore="http://www.openarchives.org/ore/terms/"
                      xmlns:owl="http://www.w3.org/2002/07/owl#"
                      xmlns:rdau="http://www.rdaregistry.info/Elements/u/"
                      xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                      xmlns:skos="http://www.w3.org/2004/02/skos/core#"
                      xmlns:svcs="http://rdfs.org/sioc/services#"
                      xmlns:wgs84_pos="http://www.w3.org/2003/01/geo/wgs84_pos#"
                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <edm:ProvidedCHO>
                 <dc:date>1301-1320</dc:date>
                 <dc:description>Четворојеванђеље (без Теофилактових предговора) и праксапостол, са синаксарима, месецословима и распоредима читања у разним приликама.</dc:description>
                 <dc:description>Са опсецањем маргина одсечене су и ознаке свешчица, осим 19. и 10.</dc:description>
                 <dc:description>Недостаје 1 л. на почетку, 1 л. иза л. 3, 1 л. иза л. 4, цела св. (8 л.) иза л. 14, 1 л. иза л. 29, 1 л. иза л. 165 и 1 или 2 л. на крају. На л. 2 одсечена је десна страна, на л. 3 и 4 доња маргина, док су од л. 23-25 и 29 остали само одерци.</dc:description>
                 <dc:description>Уставно писмо; два писара.</dc:description>
                 <dc:description>Заставица од кругова са вегетабилним изданцима који образују крст.</dc:description>
                 <dc:description>Киноварни иницијали.</dc:description>
                 <dc:description>Више записа на л. 1r: о упокојењу презвитера Витомира, затим, помен неког Михаила Бугарина, као и вежба руке.</dc:description>
                 <dc:description>Крст на Голготи у централном пољу предње корице.</dc:description>
                 <dc:description>дрво, кожа 27,8215</dc:description>
                 <dc:description>Недостају споне са копчама.</dc:description>
                 <dc:identifier>Рс 643 </dc:identifier>
                 <dc:language>cu</dc:language>
                 <dc:title>Четворојеванђеље и праксапостол</dc:title>
                 <dc:format xml:lang="en">codex</dc:format>
                 <dcterms:created>1301-1320</dcterms:created>
                 <dcterms:created xml:lang="sr">почетак XIV в.</dcterms:created>
                 <dcterms:extent>21,2 x 28,2</dcterms:extent>
                 <dcterms:isReferencedBy>Љ. Стојановић, Каталог Народне библиотеке у Београду. IV Рукописи и старе штампане књиге, Београд 1903, бр. 92, 31-32; Љ. Штављанин-Ђорђевић, М. Гроздановић-Пајић, Опис ћирилских рукописа Народне библиотеке Србије. Књига прва, Београд 1986, 329-331. </dcterms:isReferencedBy>
                 <dcterms:medium xml:lang="en">parchment</dcterms:medium>
                 <dcterms:spatial xml:lang="sr">Србија</dcterms:spatial>
                 <edm:currentLocation>Belgrade</edm:currentLocation>
                 <edm:type>TEXT</edm:type>
              </edm:ProvidedCHO>
              <edm:WebResource rdf:about="https://imagines.manuscriptorium.com/loris/NLS___-NLS2__RS_643______06VMZI9-sr/id_001/full/full/0/default.jpg">
                 <dc:format>image/jpg</dc:format>
                 <dc:type>text</dc:type>
                 <dc:type>images</dc:type>
                 <dcterms:conformsTo>TEI P5 ENRICH Schema</dcterms:conformsTo>
                 <edm:rights rdf:resource="http://creativecommons.org/licenses/by-nc-sa/4.0/"/>
                 <dcterms:isReferencedBy rdf:resource="https://collectiones.manuscriptorium.com/assorted/NLS___/NLS2__/9/NLS___-NLS2__RS_643______06VMZI9-sr"/>
                 <svcs:has_service rdf:resource="https://imagines.manuscriptorium.com/loris/NLS___-NLS2__RS_643______06VMZI9-sr/id_001/"/>
              </edm:WebResource>
              <ore:Aggregation rdf:about="http://www.manuscriptorium.com/apps/index.php?direct=record&amp;pid=NLS___-NLS2__RS_643______06VMZI9-sr">
                 <edm:aggregatedCHO rdf:resource="http://www.manuscriptorium.com/object/NLS2__RS_643______06VMZI9"/>
                 <edm:dataProvider>National library of Serbia</edm:dataProvider>
                 <edm:isShownAt rdf:resource="http://www.manuscriptorium.com/apps/index.php?direct=record&amp;pid=NLS___-NLS2__RS_643______06VMZI9-sr"/>
                 <edm:isShownBy rdf:resource="https://imagines.manuscriptorium.com/loris/NLS___-NLS2__RS_643______06VMZI9-sr/id_001/full/full/0/default.jpg"/>
                 <edm:object rdf:resource="https://imagines.manuscriptorium.com/loris/NLS___-NLS2__RS_643______06VMZI9-sr/id_001/full/!400,400/0/default.jpg"/>
                 <edm:provider>Manuscriptorium - National Library of the Czech Republic</edm:provider>
                 <edm:rights rdf:resource="http://creativecommons.org/licenses/by-nc-sa/4.0/"/>
              </ore:Aggregation>
              <svcs:Service rdf:about="https://imagines.manuscriptorium.com/loris/NLS___-NLS2__RS_643______06VMZI9-sr/id_001/">
                 <dcterms:conformsTo rdf:resource="http://iiif.io/api/image"/>
                 <doap:implements rdf:resource="http://iiif.io/api/image/2/level2.json"/>
              </svcs:Service>
          </rdf:RDF>
          """;
}