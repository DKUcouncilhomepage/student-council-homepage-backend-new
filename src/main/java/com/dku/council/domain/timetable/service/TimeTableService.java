package com.dku.council.domain.timetable.service;

import com.dku.council.domain.timetable.exception.LectureNotFoundException;
import com.dku.council.domain.timetable.exception.TimeConflictException;
import com.dku.council.domain.timetable.exception.TimeTableNotFoundException;
import com.dku.council.domain.timetable.exception.TooSmallTimeException;
import com.dku.council.domain.timetable.model.dto.TimePromise;
import com.dku.council.domain.timetable.model.dto.request.CreateTimeTableRequestDto;
import com.dku.council.domain.timetable.model.dto.request.RequestScheduleDto;
import com.dku.council.domain.timetable.model.dto.response.LectureTemplateDto;
import com.dku.council.domain.timetable.model.dto.response.ListTimeTableDto;
import com.dku.council.domain.timetable.model.dto.response.TimeTableDto;
import com.dku.council.domain.timetable.model.entity.LectureTemplate;
import com.dku.council.domain.timetable.model.entity.TimeSchedule;
import com.dku.council.domain.timetable.model.entity.TimeTable;
import com.dku.council.domain.timetable.repository.LectureTemplateRepository;
import com.dku.council.domain.timetable.repository.TimeScheduleRepository;
import com.dku.council.domain.timetable.repository.TimeTableRepository;
import com.dku.council.domain.user.model.entity.User;
import com.dku.council.domain.user.repository.UserRepository;
import com.dku.council.global.error.exception.UserNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.dku.council.domain.timetable.model.mapper.TimeScheduleMapper.createScheduleFromLecture;

@Service
@RequiredArgsConstructor
@Transactional
public class TimeTableService {

    public static final Duration MINIMUM_DURATION = Duration.of(30, ChronoUnit.MINUTES);

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final LectureTemplateRepository lectureTemplateRepository;
    private final TimeTableRepository timeTableRepository;
    private final TimeScheduleRepository timeScheduleRepository;


    @Transactional(readOnly = true)
    public Page<LectureTemplateDto> listLectures(Specification<LectureTemplate> spec, Pageable pageable) {
        Page<LectureTemplate> lectures = lectureTemplateRepository.findAll(spec, pageable);
        return lectures.map(e -> new LectureTemplateDto(objectMapper, e));
    }

    @Transactional(readOnly = true)
    public List<ListTimeTableDto> list(Long userId) {
        List<TimeTable> timeTables = timeTableRepository.findAllByUserId(userId);

        return timeTables.stream()
                .map(ListTimeTableDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TimeTableDto findOne(Long userId, Long tableId) {
        TimeTable table = timeTableRepository.findById(tableId)
                .orElseThrow(TimeTableNotFoundException::new);

        if (!table.getUser().getId().equals(userId))
            throw new TimeTableNotFoundException();

        return new TimeTableDto(objectMapper, table);
    }

    public Long create(Long userId, CreateTimeTableRequestDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        TimeTable timeTable = new TimeTable(user, dto.getName());
        appendScheduleEntity(timeTable, dto.getLectures());
        timeTable = timeTableRepository.save(timeTable);

        return timeTable.getId();
    }

    public Long update(Long userId, Long tableId, List<RequestScheduleDto> lectureDtos) {
        TimeTable table = timeTableRepository.findById(tableId)
                .orElseThrow(TimeTableNotFoundException::new);

        if (!table.getUser().getId().equals(userId))
            throw new TimeTableNotFoundException();

        table.getSchedules().clear();
        appendScheduleEntity(table, lectureDtos);

        return table.getId();
    }

    public Long updateName(Long userId, Long tableId, String name) {
        TimeTable table = timeTableRepository.findById(tableId)
                .orElseThrow(TimeTableNotFoundException::new);

        if (!table.getUser().getId().equals(userId))
            throw new TimeTableNotFoundException();

        table.changeName(name);
        return table.getId();
    }

    private void appendScheduleEntity(TimeTable table, List<RequestScheduleDto> lectureDtos) {
        List<LectureTemplate> lectures = findLectureTemplates(lectureDtos);
        Map<Long, LectureTemplate> lectureMaps = new HashMap<>();
        for (LectureTemplate lecture : lectures) {
            lectureMaps.put(lecture.getId(), lecture);
        }

        List<TimePromise> prevTimes = new ArrayList<>(lectureDtos.size() * 2);
        for (RequestScheduleDto dto : lectureDtos) {
            Long lectureId = dto.getLectureId();
            TimeSchedule schedule;
            List<TimePromise> timePromises;

            if (lectureId != null) {
                LectureTemplate lecture = lectureMaps.get(lectureId);
                schedule = createScheduleFromLecture(lecture, dto.getColor());
                timePromises = TimePromise.parse(objectMapper, schedule.getTimesJson());
            } else {
                schedule = createScheduleFromDto(dto);
                timePromises = dto.getTimes();
            }

            validateTime(prevTimes, timePromises);

            schedule = timeScheduleRepository.save(schedule);
            schedule.changeTimeTable(table);
        }
    }

    private static void validateTime(List<TimePromise> prevTimes, List<TimePromise> timePromises) {
        for (TimePromise promise : timePromises) {
            if (promise.getDuration().compareTo(MINIMUM_DURATION) < 0) {
                throw new TooSmallTimeException(promise);
            }
            for (TimePromise prev : prevTimes) {
                if (prev.isConflict(promise)) {
                    throw new TimeConflictException(prev, promise);
                }
            }
            prevTimes.add(promise);
        }
    }

    private TimeSchedule createScheduleFromDto(RequestScheduleDto dto) {
        return TimeSchedule.builder()
                .name(dto.getName())
                .memo(dto.getMemo())
                .color(dto.getColor())
                .timesJson(TimePromise.serialize(objectMapper, dto.getTimes()))
                .build();
    }

    private List<LectureTemplate> findLectureTemplates(List<RequestScheduleDto> lectureDtos) {
        List<Long> idList = lectureDtos.stream()
                .map(RequestScheduleDto::getLectureId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<LectureTemplate> lectures = lectureTemplateRepository.findAllById(idList);

        if (lectures.size() != idList.size()) {
            throw new LectureNotFoundException();
        }

        return lectures;
    }

    public Long delete(Long userId, Long tableId) {
        TimeTable table = timeTableRepository.findById(tableId)
                .orElseThrow(TimeTableNotFoundException::new);

        if (!table.getUser().getId().equals(userId))
            throw new TimeTableNotFoundException();

        timeTableRepository.delete(table);
        return table.getId();
    }
}
