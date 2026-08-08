package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.application.mapper.InterventionMapper;
import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.repository.InterventionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchInterventionUseCaseTest {

    @Mock private InterventionRepository interventionRepository;
    @Mock private InterventionMapper interventionMapper;

    @InjectMocks private SearchInterventionUseCase useCase;

    @Test
    void execute_passesIndependentFiltersToRepository() {
        UUID equipmentId = UUID.randomUUID();
        Page<Intervention> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(interventionRepository.fullTextSearch(isNull(), eq(equipmentId.toString()), eq("E001"), isNull(), any()))
                .thenReturn(page);
        when(interventionMapper.toResponseList(any())).thenReturn(List.of());

        useCase.execute(null, equipmentId, null, "E001", null, PageRequest.of(0, 20));

        verify(interventionRepository).fullTextSearch(isNull(), eq(equipmentId.toString()), eq("E001"), isNull(), any());
    }

    @Test
    void execute_combinesKeywordAndSymptomIntoTextQuery() {
        Page<Intervention> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(interventionRepository.fullTextSearch(eq("surchauffe vibration"), isNull(), isNull(), isNull(), any()))
                .thenReturn(page);
        when(interventionMapper.toResponseList(any())).thenReturn(List.of());

        useCase.execute("surchauffe", null, "vibration", null, null, PageRequest.of(0, 20));

        verify(interventionRepository).fullTextSearch(eq("surchauffe vibration"), isNull(), isNull(), isNull(), any());
    }
}
