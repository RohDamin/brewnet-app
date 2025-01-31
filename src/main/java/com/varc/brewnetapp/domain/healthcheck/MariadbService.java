package com.varc.brewnetapp.domain.healthcheck;

import com.varc.brewnetapp.domain.member.command.domain.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MariadbService {
    private final CompanyRepository companyRepository;

    @Autowired
    public MariadbService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public String getCompanyName() {
        return companyRepository.findById(1).get().getName();
    }
}
