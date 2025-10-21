package com.nckh.service;

import com.nckh.dto.request.KlangIOCallbackRequest;
import com.nckh.dto.request.SheetGanerateResquest;
import com.nckh.dto.response.SheetGanerateResponse;
import com.nckh.dto.response.SheetMusicDetailResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SheetService {
    SheetGanerateResponse generateSheet(SheetGanerateResquest resquest);
    void handleKlangIOCallback(KlangIOCallbackRequest request);
    boolean delete(Long id);
    List<SheetMusicDetailResponse> getSumaryInfo(String username);
    SheetMusicDetailResponse getDetail(Long id);
    String addLyricsToMusicXML(String musicXmlContent, String lyrics) throws Exception;
    void testTimeProcess(MultipartFile audioFile);
    void handleCallBackTest(KlangIOCallbackRequest request);
}
