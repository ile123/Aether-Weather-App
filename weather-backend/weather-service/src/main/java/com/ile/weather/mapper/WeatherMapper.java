package com.ile.weather.mapper;

import com.ile.weather.domain.entity.WeatherCurrent;
import com.ile.weather.domain.entity.WeatherLocation;
import dto.WeatherCurrentDto;
import mapper.WeatherDescriptionMapper;
import mapper.WeatherMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        config = WeatherMapperConfig.class,
        imports = { WeatherDescriptionMapper.class, java.time.ZoneOffset.class }
)
public interface WeatherMapper {

    @Mapping(target = "locationName", source = "location.name")
    @Mapping(target = "description", expression = "java(WeatherDescriptionMapper.getDescription(weatherCurrent.getWeatherCode()))")
    @Mapping(target = "recordedAt", expression = "java(weatherCurrent.getRecordedAt().toInstant(ZoneOffset.UTC))")
    WeatherCurrentDto toWeatherCurrentDto(WeatherCurrent weatherCurrent, WeatherLocation location);
}