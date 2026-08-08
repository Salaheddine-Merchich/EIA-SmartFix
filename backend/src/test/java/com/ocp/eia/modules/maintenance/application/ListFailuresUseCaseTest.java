package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.FailureDto.FailureResponse;
import com.ocp.eia.application.mapper.FailureMapper;
import com.ocp.eia.domain.model.Failure;
import com.ocp.eia.domain.repository.FailureRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListFailuresUseCaseTest {

    @Mock private FailureRepository failureRepository;
    @Mock private FailureMapper failureMapper;

    @InjectMocks private ListFailuresUseCase useCase;

    @Test
    void execute_returnsPagedResponse() {
        UUID equipmentId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);
        Failure failure = Failure.builder().id(UUID.randomUUID()).build();
        Page<Failure> page = new PageImpl<>(List.of(failure), pageable, 1);
        FailureResponse response = mock(FailureResponse.class);

        when(failureRepository.search(equipmentId, null, null, null, null, pageable)).thenReturn(page);
        when(failureMapper.toResponseList(page.getContent())).thenReturn(List.of(response));

        var result = useCase.execute(equipmentId, null, null, null, null, pageable);

        assertEquals(1, result.content().size());
        assertEquals(response, result.content().get(0));
        assertEquals(1L, result.totalElements());
    }
}
