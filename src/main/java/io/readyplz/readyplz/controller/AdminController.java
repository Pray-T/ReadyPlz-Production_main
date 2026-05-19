package io.readyplz.readyplz.controller;

import io.readyplz.readyplz.exception.GameImportException;
import io.readyplz.readyplz.service.JsonToDbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
 

import java.io.IOException;
import org.springframework.dao.DataAccessException;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
 
    private final JsonToDbService jsonToDbService;
     
    @PostMapping("/db/load-json-games")
    public ResponseEntity<String> loadGamesFromJsonFile() {
        try {
            jsonToDbService.saveDataFromJsonFile();
            return ResponseEntity.ok("JSON 파일의 게임 정보가 성공적으로 데이터베이스에 저장되었습니다.");
        } catch (GameImportException e) {
            log.error("게임 import 배치 {} 실패", e.getBatchNumber(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("게임 데이터 저장 중 배치 " + e.getBatchNumber() + "에서 오류가 발생했습니다.");
        } catch (IOException e) {
            log.error("게임 데이터 JSON 파일 읽기 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("게임 데이터 JSON 파일을 읽는 중 오류가 발생했습니다.");
        } catch (DataAccessException e) {
            log.error("게임 데이터 DB 저장 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("데이터베이스에 게임 정보를 저장하는 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("JSON 파일 처리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("JSON 파일 처리 중 알 수 없는 오류가 발생했습니다.");
        }
    }
}