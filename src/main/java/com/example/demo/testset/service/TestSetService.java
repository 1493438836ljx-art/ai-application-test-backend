package com.example.demo.testset.service;

import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.exception.ErrorCode;
import com.example.demo.testset.dto.*;
import com.example.demo.testset.entity.TestCase;
import com.example.demo.testset.entity.TestSet;
import com.example.demo.testset.mapper.TestSetMapper;
import com.example.demo.testset.repository.TestCaseRepository;
import com.example.demo.testset.repository.TestSetRepository;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestSetService {

    private final TestSetRepository testSetRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestSetMapper testSetMapper;

    @Transactional
    public TestSetResponse createTestSet(TestSetCreateRequest request) {
        TestSet testSet = testSetMapper.toEntity(request);
        testSet.setTotalCases(0);
        TestSet saved = testSetRepository.save(testSet);
        return testSetMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<TestSetResponse> getTestSets(String name, Pageable pageable) {
        Page<TestSet> testSets;
        if (StringUtils.isNotBlank(name)) {
            testSets = testSetRepository.findByNameContainingIgnoreCase(name, pageable);
        } else {
            testSets = testSetRepository.findAll(pageable);
        }
        return testSets.map(testSetMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TestSetResponse getTestSetById(Long id) {
        TestSet testSet = findTestSetById(id);
        return testSetMapper.toResponse(testSet);
    }

    @Transactional
    public TestSetResponse updateTestSet(Long id, TestSetUpdateRequest request) {
        TestSet testSet = findTestSetById(id);
        testSetMapper.updateEntity(request, testSet);
        TestSet updated = testSetRepository.save(testSet);
        return testSetMapper.toResponse(updated);
    }

    @Transactional
    public void deleteTestSet(Long id) {
        if (!testSetRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.TEST_SET_NOT_FOUND);
        }
        testSetRepository.deleteById(id);
    }

    @Transactional
    public TestCaseResponse addTestCase(Long testSetId, TestCaseCreateRequest request) {
        TestSet testSet = findTestSetById(testSetId);

        TestCase testCase = testSetMapper.toEntity(request);
        testCase.setTestSet(testSet);

        TestCase saved = testCaseRepository.save(testCase);

        updateTotalCases(testSetId);

        return testSetMapper.toResponse(saved);
    }

    @Transactional
    public List<TestCaseResponse> addTestCases(Long testSetId, List<TestCaseCreateRequest> requests) {
        TestSet testSet = findTestSetById(testSetId);

        List<TestCase> testCases = new ArrayList<>();
        for (TestCaseCreateRequest request : requests) {
            TestCase testCase = testSetMapper.toEntity(request);
            testCase.setTestSet(testSet);
            testCases.add(testCase);
        }

        List<TestCase> saved = testCaseRepository.saveAll(testCases);
        updateTotalCases(testSetId);

        return testSetMapper.toTestCaseResponseList(saved);
    }

    @Transactional(readOnly = true)
    public Page<TestCaseResponse> getTestCases(Long testSetId, Pageable pageable) {
        return testCaseRepository.findByTestSetId(testSetId, pageable)
                .map(testSetMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<TestCaseResponse> getAllTestCases(Long testSetId) {
        List<TestCase> testCases = testCaseRepository.findByTestSetIdOrderBySequence(testSetId);
        return testSetMapper.toTestCaseResponseList(testCases);
    }

    @Transactional
    public TestCaseResponse updateTestCase(Long testSetId, Long caseId, TestCaseUpdateRequest request) {
        TestCase testCase = findTestCaseById(caseId);

        if (!testCase.getTestSet().getId().equals(testSetId)) {
            throw new BusinessException(ErrorCode.TEST_CASE_NOT_FOUND, "测试用例不属于该测评集");
        }

        testSetMapper.updateEntity(request, testCase);
        TestCase updated = testCaseRepository.save(testCase);

        return testSetMapper.toResponse(updated);
    }

    @Transactional
    public void deleteTestCase(Long testSetId, Long caseId) {
        TestCase testCase = findTestCaseById(caseId);

        if (!testCase.getTestSet().getId().equals(testSetId)) {
            throw new BusinessException(ErrorCode.TEST_CASE_NOT_FOUND, "测试用例不属于该测评集");
        }

        testCaseRepository.delete(testCase);
        updateTotalCases(testSetId);
    }

    @Transactional
    public TestSetResponse importTestCases(Long testSetId, MultipartFile file) {
        TestSet testSet = findTestSetById(testSetId);

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new BusinessException(ErrorCode.TEST_SET_IMPORT_FAILED, "无法获取文件名");
        }

        List<TestCaseImportItem> items;

        try {
            if (filename.toLowerCase().endsWith(".json")) {
                items = importFromJson(file);
            } else if (filename.toLowerCase().endsWith(".csv")) {
                items = importFromCsv(file);
            } else if (filename.toLowerCase().endsWith(".xlsx") || filename.toLowerCase().endsWith(".xls")) {
                items = importFromExcel(file);
            } else {
                throw new BusinessException(ErrorCode.TEST_SET_IMPORT_FAILED, "不支持的文件格式，仅支持JSON、CSV、Excel");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Import test cases failed", e);
            throw new BusinessException(ErrorCode.TEST_SET_IMPORT_FAILED, "导入失败: " + e.getMessage());
        }

        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.TEST_SET_IMPORT_FAILED, "文件中没有有效的测试用例");
        }

        List<TestCase> testCases = testSetMapper.toTestCaseList(items);
        int sequence = testCaseRepository.findMaxSequenceByTestSetId(testSetId);
        if (sequence < 0) sequence = 0;

        for (TestCase testCase : testCases) {
            testCase.setTestSet(testSet);
            if (testCase.getSequence() == null) {
                testCase.setSequence(++sequence);
            }
        }

        testCaseRepository.saveAll(testCases);
        updateTotalCases(testSetId);

        return testSetMapper.toResponse(testSetRepository.findById(testSetId).orElse(testSet));
    }

    private List<TestCaseImportItem> importFromJson(MultipartFile file) throws IOException {
        TestSetImportRequest request = com.example.demo.common.util.JsonUtils.fromJson(
                new String(file.getBytes()),
                TestSetImportRequest.class
        );
        return request != null && request.getTestCases() != null ? request.getTestCases() : new ArrayList<>();
    }

    private List<TestCaseImportItem> importFromCsv(MultipartFile file) throws IOException, CsvException {
        List<TestCaseImportItem> items = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> rows = reader.readAll();

            boolean hasHeader = true;
            int startRow = hasHeader ? 1 : 0;

            for (int i = startRow; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length < 1) continue;

                TestCaseImportItem item = TestCaseImportItem.builder()
                        .sequence(i - startRow + 1)
                        .input(row.length > 0 ? row[0] : null)
                        .expectedOutput(row.length > 1 ? row[1] : null)
                        .tags(row.length > 2 ? row[2] : null)
                        .build();
                items.add(item);
            }
        }

        return items;
    }

    private List<TestCaseImportItem> importFromExcel(MultipartFile file) throws IOException {
        List<TestCaseImportItem> items = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            int startRow = 1;
            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                TestCaseImportItem item = TestCaseImportItem.builder()
                        .sequence(i)
                        .input(getCellValue(row.getCell(0)))
                        .expectedOutput(getCellValue(row.getCell(1)))
                        .tags(getCellValue(row.getCell(2)))
                        .extraData(getCellValue(row.getCell(3)))
                        .build();
                items.add(item);
            }
        }

        return items;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private TestSet findTestSetById(Long id) {
        return testSetRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEST_SET_NOT_FOUND));
    }

    private TestCase findTestCaseById(Long id) {
        return testCaseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEST_CASE_NOT_FOUND));
    }

    private void updateTotalCases(Long testSetId) {
        long count = testCaseRepository.countByTestSetId(testSetId);
        testSetRepository.findById(testSetId).ifPresent(testSet -> {
            testSet.setTotalCases((int) count);
            testSetRepository.save(testSet);
        });
    }
}
