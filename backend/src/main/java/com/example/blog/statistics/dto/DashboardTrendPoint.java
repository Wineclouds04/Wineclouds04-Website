package com.example.blog.statistics.dto;

import java.time.LocalDate;

public record DashboardTrendPoint(LocalDate date, long views, long visitors) {
}
