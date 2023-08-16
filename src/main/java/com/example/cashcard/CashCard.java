package com.example.cashcard;

import org.springframework.data.annotationId;

public record CashCard (@Id Long id, Double amount, String owner){
	
}