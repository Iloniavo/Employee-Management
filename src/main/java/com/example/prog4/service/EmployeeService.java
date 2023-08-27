package com.example.prog4.service;

import com.example.prog4.config.CompanyConf;
import com.example.prog4.controller.mapper.EmployeeMapper;
import com.example.prog4.model.EmployeeFilter;
import com.example.prog4.model.exception.NotFoundException;
import com.example.prog4.repository.EmployeeRepository;
import com.example.prog4.repository.dao.EmployeeManagerDao;
import com.example.prog4.repository.entity.Employee;
import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.*;
import java.util.List;

@Service
@AllArgsConstructor
public class EmployeeService {
    private EmployeeRepository repository;
    private EmployeeManagerDao employeeManagerDao;


    public Employee getOne(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Not found id=" + id));
    }

    public List<Employee> getAll(EmployeeFilter filter) {
        Sort sort = Sort.by(filter.getOrderDirection(), filter.getOrderBy().toString());
        Pageable pageable = PageRequest.of(filter.getIntPage() - 1, filter.getIntPerPage(), sort);
        return employeeManagerDao.findByCriteria(
                filter.getLastName(),
                filter.getFirstName(),
                filter.getCountryCode(),
                filter.getSex(),
                filter.getPosition(),
                filter.getEntrance(),
                filter.getDeparture(),
                pageable
        );
    }

    public void saveOne(Employee employee) {
        repository.save(employee);
    }

    private EmployeeMapper employeeMapper;
    public String parseThymeleafTemplate(com.example.prog4.model.Employee employee) {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();

        com.example.prog4.model.Employee view = employee;

        templateResolver.setSuffix(".html");
        templateResolver.setPrefix("/templates/");
        templateResolver.setTemplateMode(TemplateMode.HTML);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        Context context = new Context();
        context.setVariable("employee", view);
        context.setVariable("companyConf", new CompanyConf());

        return templateEngine.process("pdf-view", context);
    }

    public void generateAndDownloadPdf(String html, HttpServletResponse response) throws IOException, DocumentException {
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();

        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(pdfOutputStream);
        renderer.finishPDF();

        byte[] pdfBytes = pdfOutputStream.toByteArray();
        pdfOutputStream.close();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=employee.pdf");
        response.setContentLength(pdfBytes.length);

        FileCopyUtils.copy(pdfBytes, response.getOutputStream());
    }

    public void generatePdfFromHtml(String html) throws IOException, DocumentException {
        String outputFolder = System.getProperty("user.home") + File.separator + "thymeleaf.pdf";
        OutputStream outputStream = new FileOutputStream(outputFolder);

        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(outputStream);

        outputStream.close();
    }
}
