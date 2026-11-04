package com.agavide.financialapi.shared.api;

import com.agavide.financialapi.shared.domain.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(DomainException.class)
	public ProblemDetail handleDomainException(DomainException ex) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		problem.setDetail(ex.getMessage());
		return problem;
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		problem.setDetail(ex.getMessage());
		return problem;
	}

	@ExceptionHandler(ConflictException.class)
	public ProblemDetail handleConflictException(ConflictException ex) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
		problem.setDetail(ex.getMessage());
		return problem;
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleGenericException(Exception ex) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		problem.setDetail("An unexpected error occurred");
		return problem;
	}
}
