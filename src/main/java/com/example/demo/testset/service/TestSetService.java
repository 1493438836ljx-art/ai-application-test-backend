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

/**
 * 测评集服务类
 * <p>
 * 提供测评集和测试用例的业务逻辑处理，包括CRUD操作、批量导入等功能。
 * 支持从JSON、CSV、Excel文件导入测试用例。
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestSetService {

    private final TestSetRepository testSetRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestSetMapper testSetMapper;

    /**
     * 创建测评集
     *
     * @param request 创建请求
     * @return 创建后的测评集响应
     */
    @Transactional
    public TestSetResponse createTestSet(TestSetCreateRequest request) {
        TestSet testSet = testSetMapper.toEntity(request);
        // 初始化测试用例数为0
        testSet.setTotalCases(0);
        TestSet saved = testSetRepository.save(testSet);
        return testSetMapper.toResponse(saved);
    }

    /**
     * 分页查询测评集列表
     *
     * @param name     名称过滤条件（可选）
     * @param pageable 分页参数
     * @return 测评集分页结果
     */
    @Transactional(readOnly = true)
    public Page<TestSetResponse> getTestSets(String name, Pageable pageable) {
        Page<TestSet> testSets;
        if (StringUtils.isNotBlank(name)) {
            // 按名称模糊查询
            testSets = testSetRepository.findByNameContainingIgnoreCase(name, pageable);
        } else {
            // 查询全部
            testSets = testSetRepository.findAll(pageable);
        }
        return testSets.map(testSetMapper::toResponse);
    }

    /**
     * 根据ID获取测评集详情
     *
     * @param id 测评集ID
     * @return 测评集响应
     */
    @Transactional(readOnly = true)
    public TestSetResponse getTestSetById(Long id) {
        TestSet testSet = findTestSetById(id);
        return testSetMapper.toResponse(testSet);
    }

    /**
     * 更新测评集
     *
     * @param id      测评集ID
     * @param request 更新请求
     * @return 更新后的测评集响应
     */
    @Transactional
    public TestSetResponse updateTestSet(Long id, TestSetUpdateRequest request) {
        TestSet testSet = findTestSetById(id);
        testSetMapper.updateEntity(request, testSet);
        TestSet updated = testSetRepository.save(testSet);
        return testSetMapper.toResponse(updated);
    }

    /**
     * 删除测评集
     * <p>
     * 同时删除关联的所有测试用例（级联删除）
     * </p>
     *
     * @param id 测评集ID
     */
    @Transactional
    public void deleteTestSet(Long id) {
        if (!testSetRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.TEST_SET_NOT_FOUND);
        }
        testSetRepository.deleteById(id);
    }

    /**
     * 向测评集添加单个测试用例
     *
     * @param testSetId 测评集ID
     * @param request   创建请求
     * @return 创建后的测试用例响应
     */
    @Transactional
    public TestCaseResponse addTestCase(Long testSetId, TestCaseCreateRequest request) {
        TestSet testSet = findTestSetById(testSetId);

        TestCase testCase = testSetMapper.toEntity(request);
        testCase.setTestSet(testSet);

        TestCase saved = testCaseRepository.save(testCase);

        // 更新测试用例总数
        updateTotalCases(testSetId);

        return testSetMapper.toResponse(saved);
    }

    /**
     * 向测评集批量添加测试用例
     *
     * @param testSetId 测评集ID
     * @param requests  创建请求列表
     * @return 创建后的测试用例响应列表
     */
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
        // 更新测试用例总数
        updateTotalCases(testSetId);

        return testSetMapper.toTestCaseResponseList(saved);
    }

    /**
     * 分页查询测评集下的测试用例
     *
     * @param testSetId 测评集ID
     * @param pageable  分页参数
     * @return 测试用例分页结果
     */
    @Transactional(readOnly = true)
    public Page<TestCaseResponse> getTestCases(Long testSetId, Pageable pageable) {
        return testCaseRepository.findByTestSetId(testSetId, pageable)
                .map(testSetMapper::toResponse);
    }

    /**
     * 获取测评集下的所有测试用例（不分页）
     *
     * @param testSetId 测评集ID
     * @return 测试用例列表
     */
    @Transactional(readOnly = true)
    public List<TestCaseResponse> getAllTestCases(Long testSetId) {
        List<TestCase> testCases = testCaseRepository.findByTestSetIdOrderBySequence(testSetId);
        return testSetMapper.toTestCaseResponseList(testCases);
    }

    /**
     * 更新测试用例
     *
     * @param testSetId 测评集ID
     * @param caseId    测试用例ID
     * @param request   更新请求
     * @return 更新后的测试用例响应
     */
    @Transactional
    public TestCaseResponse updateTestCase(Long testSetId, Long caseId, TestCaseUpdateRequest request) {
        TestCase testCase = findTestCaseById(caseId);

        // 验证测试用例属于指定测评集
        if (!testCase.getTestSet().getId().equals(testSetId)) {
            throw new BusinessException(ErrorCode.TEST_CASE_NOT_FOUND, "测试用例不属于该测评集");
        }

        testSetMapper.updateEntity(request, testCase);
        TestCase updated = testCaseRepository.save(testCase);

        return testSetMapper.toResponse(updated);
    }

    /**
     * 删除测试用例
     *
     * @param testSetId 测评集ID
     * @param caseId    测试用例ID
     */
    @Transactional
    public void deleteTestCase(Long testSetId, Long caseId) {
        TestCase testCase = findTestCaseById(caseId);

        // 验证测试用例属于指定测评集
        if (!testCase.getTestSet().getId().equals(testSetId)) {
            throw new BusinessException(ErrorCode.TEST_CASE_NOT_FOUND, "测试用例不属于该测评集");
        }

        testCaseRepository.delete(testCase);
        // 更新测试用例总数
        updateTotalCases(testSetId);
    }

    /**
     * 从文件导入测试用例
     * <p>
     * 支持的文件格式：JSON、CSV、Excel（.xlsx/.xls）
     * </p>
     *
     * @param testSetId 测评集ID
     * @param file      导入文件
     * @return 更新后的测评集响应
     */
    @Transactional
    public TestSetResponse importTestCases(Long testSetId, MultipartFile file) {
        TestSet testSet = findTestSetById(testSetId);

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new BusinessException(ErrorCode.TEST_SET_IMPORT_FAILED, "无法获取文件名");
        }

        List<TestCaseImportItem> items;

        try {
            // 根据文件扩展名选择解析方式
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
        // 获取当前最大序号
        int sequence = testCaseRepository.findMaxSequenceByTestSetId(testSetId);
        if (sequence < 0) sequence = 0;

        // 设置测评集关联和自动序号
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

    /**
     * 从JSON文件导入测试用例
     *
     * @param file JSON文件
     * @return 测试用例导入项列表
     */
    private List<TestCaseImportItem> importFromJson(MultipartFile file) throws IOException {
        TestSetImportRequest request = com.example.demo.common.util.JsonUtils.fromJson(
                new String(file.getBytes()),
                TestSetImportRequest.class
        );
        return request != null && request.getTestCases() != null ? request.getTestCases() : new ArrayList<>();
    }

    /**
     * 从CSV文件导入测试用例
     * <p>
     * CSV格式：input, expectedOutput, tags（第一行为表头）
     * </p>
     *
     * @param file CSV文件
     * @return 测试用例导入项列表
     */
    private List<TestCaseImportItem> importFromCsv(MultipartFile file) throws IOException, CsvException {
        List<TestCaseImportItem> items = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> rows = reader.readAll();

            // 跳过表头
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

    /**
     * 从Excel文件导入测试用例
     * <p>
     * Excel格式：
     * - 第1列：input
     * - 第2列：expectedOutput
     * - 第3列：tags
     * - 第4列：extraData
     * （第一行为表头）
     * </p>
     *
     * @param file Excel文件
     * @return 测试用例导入项列表
     */
    private List<TestCaseImportItem> importFromExcel(MultipartFile file) throws IOException {
        List<TestCaseImportItem> items = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // 跳过表头
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

    /**
     * 获取Excel单元格的字符串值
     *
     * @param cell 单元格
     * @return 字符串值
     */
    private String getCellValue(Cell cell) {
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    /**
     * 根据ID查找测评集
     *
     * @param id 测评集ID
     * @return 测评集实体
     * @throws BusinessException 测评集不存在时抛出
     */
    private TestSet findTestSetById(Long id) {
        return testSetRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEST_SET_NOT_FOUND));
    }

    /**
     * 根据ID查找测试用例
     *
     * @param id 测试用例ID
     * @return 测试用例实体
     * @throws BusinessException 测试用例不存在时抛出
     */
    private TestCase findTestCaseById(Long id) {
        return testCaseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEST_CASE_NOT_FOUND));
    }

    /**
     * 更新测评集的测试用例总数
     *
     * @param testSetId 测评集ID
     */
    private void updateTotalCases(Long testSetId) {
        long count = testCaseRepository.countByTestSetId(testSetId);
        testSetRepository.findById(testSetId).ifPresent(testSet -> {
            testSet.setTotalCases((int) count);
            testSetRepository.save(testSet);
        });
    }
}
