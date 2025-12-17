package dexter.banking.limit.service;

import cz.jirutka.rsql.parser.ast.Node;
import dexter.banking.limit.web.dto.PayeeDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Optional;

public interface PayeeQueryService {

    Page<PayeeDto> list(Node filter, Sort sort, Pageable pageable);

    Page<PayeeDto> listOnline(Node filter, Sort sort, Pageable pageable);

    Optional<PayeeDto> getOne(String id);
}