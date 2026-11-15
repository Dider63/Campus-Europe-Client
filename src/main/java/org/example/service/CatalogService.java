package org.example.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CatalogService {
    private final CatalogRepository catalogRepository;

    public CatalogService(CatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    public List<Map<String, Object>> countries() {
        return catalogRepository.countries(true);
    }

    public List<Map<String, Object>> visaTypes() {
        return catalogRepository.visaTypes(true);
    }

    public List<Map<String, Object>> catalog(Long destination, Long nationality, Long type) {
        return catalogRepository.catalog(destination, nationality, type);
    }

    public Map<String, Object> rule(long id) {
        return catalogRepository.catalogItem(id);
    }

    public List<Map<String, Object>> checklist(long id) {
        return catalogRepository.checklist(id);
    }
}

