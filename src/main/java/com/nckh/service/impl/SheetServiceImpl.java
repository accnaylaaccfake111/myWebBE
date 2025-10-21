package com.nckh.service.impl;

import com.nckh.dto.request.KlangIOCallbackRequest;
import com.nckh.dto.request.SheetGanerateResquest;
import com.nckh.dto.response.KlangIOResponse;
import com.nckh.dto.response.SheetGanerateResponse;
import com.nckh.dto.response.SheetMusicDetailResponse;
import com.nckh.dto.response.SheetMusicResponse;
import com.nckh.entity.MediaFile;
import com.nckh.entity.MusicGenTask;
import com.nckh.entity.SheetMusic;
import com.nckh.entity.User;
import com.nckh.httpclient.KlangIOClient;
import com.nckh.repository.MusicGenTaskRepository;
import com.nckh.repository.SheetMusicRepository;
import com.nckh.repository.UserRepository;
import com.nckh.service.CloudinaryService;
import com.nckh.service.SheetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SheetServiceImpl implements SheetService{
    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final SheetMusicRepository sheetMusicRepository;
    private final KlangIOClient klangIOClient;
    private final CloudinaryService cloudinaryService;
    private final MusicGenTaskRepository musicGenTaskRepository;

    @Value("${ai-service.klangio.api-key}")
    private String klangApiKey;
    @Value("${ai-service.klangio.url}")
    private String klangUrl;
    @Value("${ai-service.klangio.callback-url}")
    private String klangCallBackUrl;

    @Override
    public SheetGanerateResponse generateSheet(SheetGanerateResquest resquest) {
        User user = userRepository.findByUsername(resquest.getUserName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        SheetMusic sheetMusic = sheetMusicRepository.findById(resquest.getSheetMusicId())
                .orElseThrow(() -> new RuntimeException("Sheet music not found"));

        try {
            log.info("START GENERATE SHEET MUSIC PROCESSING: {}", LocalDateTime.now());
            byte[] audio = downloadMusic(sheetMusic.getAudioFile().getFileUrl());

            MultipartFile multipartFile = new MockMultipartFile(
                    "file",
                    "audiofile.mp3",
                    "audio/mpeg",
                    audio
            );

            ResponseEntity<KlangIOResponse> response = klangIOClient.transcribe(
                    klangApiKey,
                    "detect",
                    sheetMusic.getTitle(),
                    user.getFullName(),
                    klangCallBackUrl,
                    multipartFile,
                    "mxml"
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                sheetMusic.setTaskId(response.getBody().getJobId());
                sheetMusic.setStatus(SheetMusic.SheetMusicStatus.SHEET_GENERATE_PROCESSING);
                sheetMusicRepository.save(sheetMusic);
                return SheetGanerateResponse.builder()
                        .sheetMusicId(sheetMusic.getId())
                        .status("SHEET_GENERATE_PROCESSING")
                        .message("Sheet music generation request accepted")
                        .build();
            } else {
                return SheetGanerateResponse.builder()
                        .sheetMusicId(sheetMusic.getId())
                        .status("FAILED")
                        .message("Failed to initiate sheet music generation")
                        .errorMessage("HTTP Status: " + response.getStatusCode())
                        .build();
            }
        } catch (RestClientException e) {
            log.error("Error generating sheet music: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate sheet music", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional
    public void handleKlangIOCallback(KlangIOCallbackRequest request) {
        log.info("Handling KlangIO callback for ID: {}", request.getId());

        SheetMusic sheetMusic = sheetMusicRepository.findByTaskId(request.getId())
                .orElseThrow(() -> new RuntimeException("Sheet music not found for TaskID: " + request.getId()));

        if ("COMPLETED".equalsIgnoreCase(request.getStatus())) {
            try {

                ResponseEntity<String> response = klangIOClient.downloadMusicXml(request.getId(), klangApiKey);
                String musicXml = response.getBody();
                musicXml = addLyricsToMusicXML(musicXml, sheetMusic.getLyricsComposition().getLyrics());
                sheetMusic.setSheetMusicXML(musicXml);
                sheetMusic.setSheetMidelAI("KlangIO AI");
                sheetMusic.setStatus(SheetMusic.SheetMusicStatus.SHEET_COMPLETED);
                sheetMusic.setCompletedAt(java.time.LocalDateTime.now());
                sheetMusicRepository.save(sheetMusic);

                log.info("Sheet music generation completed for ID: {}", sheetMusic.getId());
            } catch (Exception e) {
                log.error("Error processing completed callback: {}", e.getMessage(), e);
                sheetMusic.setStatus(SheetMusic.SheetMusicStatus.SHEET_FAILED);
                sheetMusic.setErrorMessage("Error processing MusicXML: " + e.getMessage());
                sheetMusicRepository.save(sheetMusic);
                sheetMusicRepository.save(sheetMusic);
            }
        } else if ("FAILED".equals(request.getStatus())) {
            log.error("Sheet music generation failed for ID: {}. Error: {}", sheetMusic.getId(), request.getError());
            sheetMusic.setStatus(SheetMusic.SheetMusicStatus.SHEET_FAILED);
            sheetMusic.setErrorMessage("Generation failed: " + request.getError());
            sheetMusicRepository.save(sheetMusic);
        } else {
            log.warn("Received unknown status '{}' for ID: {}", request.getStatus(), sheetMusic.getId());
        }
        log.info("END GENERATE SHEET MUSIC PROCESSING: {}", LocalDateTime.now());
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        try {
            SheetMusic sheetMusic = sheetMusicRepository.findById(id)
                    .orElseThrow(() ->  new RuntimeException("Sheet music not found"));

            if(sheetMusic.getAudioFile() != null){
                cloudinaryService.deleteFile(
                        sheetMusic.getAudioFile().getPublicId(), "video");
            }

            if(sheetMusic.getTaskId() != null){
                MusicGenTask task = musicGenTaskRepository.findByTaskId(sheetMusic.getTaskId())
                        .orElseThrow(() -> new RuntimeException("Cannot find Music generation task"));

                musicGenTaskRepository.delete(task);

            }
            sheetMusicRepository.delete(sheetMusic);
            return true;
        }catch (IOException e){
            log.error("Delete File on Cloudinary failed");
            return false;
        }
    }

    @Override
    @Transactional
    public List<SheetMusicDetailResponse> getSumaryInfo(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return sheetMusicRepository.findAllByUser(user).stream()
                .map(this::mapToSheetMusicResponse)
                .toList();
    }

    @Override
    @Transactional
    public SheetMusicDetailResponse getDetail(Long id) {
        SheetMusic sheetMusic = sheetMusicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sheet Music with ID::%s not found".formatted(id)));
        return mapToSheetMusicResponse(sheetMusic);
    }

    @Transactional
    protected SheetMusicDetailResponse mapToSheetMusicResponse(SheetMusic sheetMusic){
        return SheetMusicDetailResponse.builder()
                .id(sheetMusic.getId())
                .title(sheetMusic.getTitle())
                .status(sheetMusic.getStatus())
                .processingTimeMs(sheetMusic.getProcessingTimeMs())
                .errorMessage(sheetMusic.getErrorMessage())
                .audioFile(Optional.ofNullable(sheetMusic.getAudioFile())
                        .map(MediaFile::getFileUrl)
                        .orElse(null))
                .sheetMusicXML(sheetMusic.getSheetMusicXML())
                .lyricModelAi("Gemini 2.5 Flash")
                .musicModelAI(sheetMusic.getMusicModelAI())
                .sheetMidelAI(sheetMusic.getSheetMidelAI())
                .completedAt(sheetMusic.getCompletedAt())
                .createdAt(sheetMusic.getCreatedAt())
                .lyrics(sheetMusic.getLyricsComposition().getLyrics())
                .build();
    };

    public byte[] downloadMusic(String audioUrl) {
        log.info("Downloading music from: {}", audioUrl);

        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(audioUrl, byte[].class);

            if (response.getBody() == null) {
                throw new RuntimeException("Empty response when downloading music");
            }

            log.info("Successfully downloaded music, size: {} bytes", response.getBody().length);
            return response.getBody();

        } catch (RestClientException e) {
            log.error("Error downloading music from {}: {}", audioUrl, e.getMessage(), e);
            throw new RuntimeException("Failed to download music", e);
        }
    }

    /**
     * Ghép lyrics vào MusicXML string
     */
    public String addLyricsToMusicXML(String musicXmlContent, String lyrics) throws Exception {
        Document doc = parseXML(musicXmlContent);
        List<String> syllables = parseLyrics(lyrics);
        List<Element> notes = getAllValidNotes(doc);
        attachLyricsToNotes(doc, notes, syllables);
        return documentToString(doc);
    }

    @Override
    public void testTimeProcess(MultipartFile audioFile) {
        ResponseEntity<KlangIOResponse> response = klangIOClient.transcribe(
                klangApiKey,
                "detect",
                "TEST",
                "TEST",
                klangCallBackUrl,
                audioFile,
                "mxml"
        );
        log.info("SEND REQUEST GENERATE SHEET AT: {}", LocalDateTime.now());
    }

    @Override
    public void handleCallBackTest(KlangIOCallbackRequest request) {
        ResponseEntity<String> response = klangIOClient.downloadMusicXml(request.getId(), klangApiKey);
        log.info("REQUEST CALL BACK AT: {}", LocalDateTime.now());
    }

    /**
     * Parse XML string thành Document
     */
    private Document parseXML(String xmlContent) throws Exception {
        // Thêm XML declaration nếu chưa có
        if (!xmlContent.trim().startsWith("<?xml")) {
            xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xmlContent;
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return (Document) builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Tách lyrics thành các âm tiết
     */
    private List<String> parseLyrics(String lyrics) {
        List<String> syllables = new ArrayList<>();
        String[] lines = lyrics.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Tách theo khoảng trắng
            String[] words = line.split("\\s+");
            for (String word : words) {
                if (!word.isEmpty()) {
                    syllables.add(word);
                }
            }
        }

        return syllables;
    }

    /**
     * Lấy tất cả note hợp lệ (có pitch, không phải rest, tied, chord)
     */
    private List<Element> getAllValidNotes(Document doc) {
        List<Element> validNotes = new ArrayList<>();
        NodeList measures = doc.getElementsByTagName("measure");

        for (int i = 0; i < measures.getLength(); i++) {
            Element measure = (Element) measures.item(i);
            NodeList notes = measure.getElementsByTagName("note");

            for (int j = 0; j < notes.getLength(); j++) {
                Element note = (Element) notes.item(j);

                // Bỏ qua tag không cần thiết
                if (hasElement(note, "rest")) continue;
                if (hasElement(note, "chord")) continue;
                if (hasElement(note, "grace")) continue;
                if (isTiedStop(note)) continue;
                if (hasElement(note, "pitch")) {
                    validNotes.add(note);
                }
            }
        }

        return validNotes;
    }

    /**
     * Kiểm tra element có tag con không
     */
    private boolean hasElement(Element parent, String tagName) {
        return parent.getElementsByTagName(tagName).getLength() > 0;
    }

    /**
     * Kiểm tra note có phải tied stop không (note được nối từ note trước)
     */
    private boolean isTiedStop(Element note) {
        NodeList notations = note.getElementsByTagName("notations");
        if (notations.getLength() == 0) return false;

        Element notation = (Element) notations.item(0);
        NodeList tieds = notation.getElementsByTagName("tied");

        for (int i = 0; i < tieds.getLength(); i++) {
            Element tied = (Element) tieds.item(i);
            String type = tied.getAttribute("type");
            if ("stop".equals(type)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Ghép lyrics vào notes
     */
    private void attachLyricsToNotes(Document doc, List<Element> notes, List<String> syllables) {
        int minSize = Math.min(notes.size(), syllables.size());

        for (int i = 0; i < minSize; i++) {
            Element note = notes.get(i);
            String syllable = syllables.get(i);

            // Xóa lyrics cũ nếu có
            removeExistingLyrics(note);

            // Tạo lyric element
            Element lyric = createLyricElement(doc, syllable, i, syllables);

            // Thêm vào note (sau notations nếu có)
            insertLyricElement(note, lyric);
        }

        // Log warning
        if (notes.size() > syllables.size()) {
            System.out.println("⚠ Cảnh báo: Có " + (notes.size() - syllables.size()) + " nốt nhạc không có lời");
        } else if (syllables.size() > notes.size()) {
            System.out.println("⚠ Cảnh báo: Có " + (syllables.size() - notes.size()) + " âm tiết không được ghép");
        }
    }

    /**
     * Xóa lyrics cũ
     */
    private void removeExistingLyrics(Element note) {
        NodeList lyrics = note.getElementsByTagName("lyric");
        List<Node> toRemove = new ArrayList<>();

        for (int i = 0; i < lyrics.getLength(); i++) {
            toRemove.add(lyrics.item(i));
        }

        for (Node lyric : toRemove) {
            note.removeChild(lyric);
        }
    }

    /**
     * Tạo lyric element theo chuẩn MusicXML
     */
    private Element createLyricElement(Document doc, String syllable, int index, List<String> allSyllables) {
        Element lyric = doc.createElement("lyric");
        lyric.setAttribute("number", "1");
        lyric.setAttribute("default-y", "-80");

        // Xác định syllabic type
        String syllabicType = determineSyllabicType(syllable, index, allSyllables);
        Element syllabicEl = doc.createElement("syllabic");
        syllabicEl.setTextContent(syllabicType);
        lyric.appendChild(syllabicEl);

        // Text content
        Element textEl = doc.createElement("text");
        String cleanText = syllable.replace("-", "");
        textEl.setTextContent(cleanText);
        lyric.appendChild(textEl);

        return lyric;
    }

    /**
     * Xác định loại syllabic
     */
    private String determineSyllabicType(String syllable, int index, List<String> syllables) {
        boolean hasDashAtEnd = syllable.endsWith("-");
        boolean prevHasDash = index > 0 && syllables.get(index - 1).endsWith("-");

        if (hasDashAtEnd && prevHasDash) {
            return "middle";
        } else if (hasDashAtEnd) {
            return "begin";
        } else if (prevHasDash) {
            return "end";
        } else {
            return "single";
        }
    }

    /**
     * Insert lyric vào đúng vị trí trong note
     */
    private void insertLyricElement(Element note, Element lyric) {
        // Tìm vị trí thích hợp (sau notations hoặc cuối cùng)
        NodeList notations = note.getElementsByTagName("notations");

        if (notations.getLength() > 0) {
            Node notationsNode = notations.item(0);
            Node nextSibling = notationsNode.getNextSibling();
            if (nextSibling != null) {
                note.insertBefore(lyric, nextSibling);
            } else {
                note.appendChild(lyric);
            }
        } else {
            note.appendChild(lyric);
        }
    }

    /**
     * Convert Document về String với format đẹp
     */
    private String documentToString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    /**
     * Ghép nhiều dòng lyrics (cho verse, chorus khác nhau)
     */
    public String addMultipleLyricsLines(String musicXmlContent, Map<Integer, String> lyricsMap) throws Exception {
        Document doc = parseXML(musicXmlContent);
        List<Element> notes = getAllValidNotes(doc);

        for (Map.Entry<Integer, String> entry : lyricsMap.entrySet()) {
            int lineNumber = entry.getKey();
            String lyrics = entry.getValue();
            List<String> syllables = parseLyrics(lyrics);

            attachLyricsWithLineNumber(doc, notes, syllables, lineNumber);
        }

        return documentToString(doc);
    }

    /**
     * Ghép lyrics với line number cụ thể
     */
    private void attachLyricsWithLineNumber(Document doc, List<Element> notes,
                                            List<String> syllables, int lineNumber) {
        int minSize = Math.min(notes.size(), syllables.size());

        for (int i = 0; i < minSize; i++) {
            Element note = notes.get(i);
            String syllable = syllables.get(i);

            Element lyric = doc.createElement("lyric");
            lyric.setAttribute("number", String.valueOf(lineNumber));
            lyric.setAttribute("default-y", String.valueOf(-80 - (lineNumber - 1) * 12));

            String syllabicType = determineSyllabicType(syllable, i, syllables);
            Element syllabicEl = doc.createElement("syllabic");
            syllabicEl.setTextContent(syllabicType);
            lyric.appendChild(syllabicEl);

            Element textEl = doc.createElement("text");
            textEl.setTextContent(syllable.replace("-", ""));
            lyric.appendChild(textEl);

            insertLyricElement(note, lyric);
        }
    }

    /**
     * Xóa tất cả lyrics khỏi MusicXML
     */
    public String removeAllLyrics(String musicXmlContent) throws Exception {
        Document doc = parseXML(musicXmlContent);
        NodeList allNotes = doc.getElementsByTagName("note");

        for (int i = 0; i < allNotes.getLength(); i++) {
            Element note = (Element) allNotes.item(i);
            removeExistingLyrics(note);
        }

        return documentToString(doc);
    }

}
