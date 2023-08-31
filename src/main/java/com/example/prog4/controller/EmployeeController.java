package com.example.prog4.controller;

import com.example.prog4.controller.mapper.EmployeeMapper;
import com.example.prog4.controller.validator.EmployeeValidator;
import com.example.prog4.model.Employee;
import com.example.prog4.model.EmployeeFilter;
import com.example.prog4.model.enums.AgeOption;
import com.example.prog4.service.CSVUtils;
import com.example.prog4.service.EmployeeService;
import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Controller
@AllArgsConstructor
@RequestMapping("/server/employee")
public class EmployeeController {
    private EmployeeMapper employeeMapper;
    private EmployeeValidator employeeValidator;
    private EmployeeService employeeService;

    @GetMapping("/list/csv")
    public ResponseEntity<byte[]> getCsv(HttpSession session, @RequestParam("ageOption")AgeOption ageOption) {
        EmployeeFilter filters = (EmployeeFilter) session.getAttribute("employeeFiltersSession");
        List<Employee> data = employeeService.getAll(filters).stream().map(employee -> employeeMapper.toView(employee, ageOption)).toList();

        String csv = CSVUtils.convertToCSV(data);
        byte[] bytes = csv.getBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "employees.csv");
        headers.setContentLength(bytes.length);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/list/filters/clear")
    public String clearFilters(HttpSession session) {
        session.removeAttribute("employeeFilters");
        return "redirect:/employee/list";
    }

    @PostMapping("/createOrUpdate")
    public String saveOne(@ModelAttribute Employee employee) {
        employeeValidator.validate(employee);
        com.example.prog4.repository.entity.Employee domain = employeeMapper.toDomain(employee);
        employeeService.saveOne(domain);
        return "redirect:/employee/list";
    }

    @GetMapping("/pdf/download/{eId}/one")
    public String downloadPdf(@PathVariable String eId, Model model, HttpServletResponse response) throws IOException, DocumentException {
        String html = employeeService.parseThymeleafTemplate(employeeMapper.toView(employeeService.getOne(eId), AgeOption.one));

        employeeService.generateAndDownloadPdf(html, response);
        return "redirect:/employee/list";
    }

    @GetMapping("/pdf/download/{eId}/two")
    public String downloadPdf1(@PathVariable String eId, Model model, HttpServletResponse response) throws IOException, DocumentException {
        String html = employeeService.parseThymeleafTemplate(employeeMapper.toView(employeeService.getOne(eId), AgeOption.two));

        employeeService.generateAndDownloadPdf(html, response);
        return "redirect:/employee/list";
    }


    private String readHtmlFile(String filePath) throws IOException {
        Resource resource = new ClassPathResource(filePath);
        byte[] bytes = StreamUtils.copyToByteArray(resource.getInputStream());
        return new String(bytes);
    }
}
