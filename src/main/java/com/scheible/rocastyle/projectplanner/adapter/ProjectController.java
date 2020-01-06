package com.scheible.rocastyle.projectplanner.adapter;

import static com.scheible.rocastyle.projectplanner.domain.ProjectService.Project.Order.AFTER;
import static com.scheible.rocastyle.projectplanner.domain.ProjectService.Project.Order.BEFORE;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.servlet.ModelAndView;

import com.scheible.rocastyle.projectplanner.domain.ProjectService;
import com.scheible.rocastyle.projectplanner.domain.ProjectService.Project;
import com.scheible.rocastyle.projectplanner.domain.ProjectService.Project.Order;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 *
 * @author sj
 */
@Controller
class ProjectController {

	private final ProjectService projectService;

	ProjectController(final ProjectService projectService) {
		this.projectService = projectService;
	}

	@GetMapping("/project.html")
	String showProject(final long id, final Model model) {
		final Project project = projectService.findOrCreateProject(id);

		model.addAttribute("projectId", project.getId());
		model.addAttribute("projectName", project.getName());
		model.addAttribute("projectRevision", project.getRevision());

		model.addAttribute("milestones", project.getMilestones().stream()
				.map(p -> new SimpleImmutableEntry<>(p.getId(), p.getName())).collect(Collectors.toList()));

		final List<Entry<String, String>> milestonePositions = project.getMilestones().stream()
				.flatMap(p -> Stream.of(
						new SimpleImmutableEntry<>(BEFORE.name() + "-" + p.getId(), "Before '" + p.getName() + "'"),
						new SimpleImmutableEntry<>(AFTER.name() + "-" + p.getId(), "After '" + p.getName() + "'")))
				.collect(Collectors.toList());
		model.addAttribute("milestonePositions", milestonePositions);

		final Function<Long, List<Entry<Long, String>>> tasksFunction = milestoneId -> project.getMilestones().stream()
				.filter(p -> p.getId() == milestoneId)
				.flatMap(p -> p.getTasks().stream().map(t -> new SimpleImmutableEntry<>(t.getId(), t.getName())))
				.collect(Collectors.toList());
		model.addAttribute("tasksFunction", tasksFunction);

		final Function<Long, List<Entry<String, String>>> taskPositionsFunction = milestoneId -> project.getMilestones()
				.stream().filter(p -> p.getId() == milestoneId)
				.flatMap(p -> p.getTasks().stream().flatMap(t -> Stream.of(
						new SimpleImmutableEntry<>(BEFORE.name() + "-" + t.getId(), "Before '" + t.getName() + "'"),
						new SimpleImmutableEntry<>(AFTER.name() + "-" + t.getId(), "After '" + t.getName() + "'"))))
				.collect(Collectors.toList());
		model.addAttribute("taskPositionsFunction", taskPositionsFunction);

		final BiFunction<Long, Long, String> taskUrlFunction = (milestoneId, taskId) -> project.getMilestones().stream()
				.filter(p -> p.getId() == milestoneId).flatMap(p -> p.getTasks().stream())
				.filter(t -> t.getId() == taskId).map(t -> t.getUrl() != null ? t.getUrl() : "").findFirst().orElse("");
		model.addAttribute("taskUrlFunction", taskUrlFunction);

		return "project";
	}

	@PutMapping("/project")
	ModelAndView updateProject(final @RequestHeader(name = "X-Requested-With", required = false) String requestedWith,
			final long projectId, final long projectRevision, final String name) {
		final Project project = projectService.findOrCreateProject(projectId);
		project.setRevision(projectRevision);
		project.setName(name);
		return toSuccessView(requestedWith, () -> "project.html?id=" + projectId);
	}

	@PostMapping("/project/milestones")
	ModelAndView addMilestone(final @RequestHeader(name = "X-Requested-With", required = false) String requestedWith,
			final long projectId, final long projectRevision, final String name, final String position) {
		if (!name.isBlank()) {
			final Project project = projectService.findOrCreateProject(projectId);
			project.setRevision(projectRevision);
			project.addMilestone(name, parseAnchor(position));
			return toSuccessView(requestedWith, () -> "project.html?id=" + projectId);
		} else {
			return toErrorView("Milestone name can't be blank!");
		}
	}

	@DeleteMapping("/project/milestones")
	ModelAndView deleteMilestone(final @RequestHeader(name = "X-Requested-With", required = false) String requestedWith,
			final long projectId, final long projectRevision, final long milestoneId) {
		final Project project = projectService.findOrCreateProject(projectId);
		project.setRevision(projectRevision);
		project.deleteMilestone(milestoneId);
		return toSuccessView(requestedWith, () -> "project.html?id=" + projectId);
	}

	@PostMapping("/project/tasks")
	ModelAndView addTask(final @RequestHeader(name = "X-Requested-With", required = false) String requestedWith,
			final long projectId, final long projectRevision, final long milestoneId, final String name,
			final String position) {
		if (!name.isBlank()) {
			final Project project = projectService.findOrCreateProject(projectId);
			project.setRevision(projectRevision);
			project.addTask(milestoneId, name, parseAnchor(position));
			return toSuccessView(requestedWith, () -> "project.html?id=" + projectId);
		} else {
			return toErrorView("Task name can't be blank!");
		}
	}

	@DeleteMapping("/project/tasks")
	ModelAndView deleteTask(final @RequestHeader(name = "X-Requested-With", required = false) String requestedWith,
			final long projectId, final long projectRevision, final long milestoneId, final long taskId) {
		final Project project = projectService.findOrCreateProject(projectId);
		project.setRevision(projectRevision);
		project.deleteTask(milestoneId, taskId);
		return toSuccessView(requestedWith, () -> "project.html?id=" + projectId);
	}

	@PutMapping("/project/tasks")
	ModelAndView updateTask(final @RequestHeader(name = "X-Requested-With", required = false) String requestedWith,
			final long projectId, final long projectRevision, final long milestoneId, final long taskId,
			final String url) {
		final Project project = projectService.findOrCreateProject(projectId);
		project.setRevision(projectRevision);
		project.changeTaskUrl(milestoneId, taskId, url);
		return toSuccessView(requestedWith, () -> "project.html?id=" + projectId);
	}

	private static Optional<Entry<Order, Long>> parseAnchor(final String position) {
		return Optional.ofNullable(position).map(p -> {
			final String[] parts = p.split("-");
			return new SimpleImmutableEntry<>(Order.valueOf(parts[0]), Long.valueOf(parts[1]));
		});
	}

	@SuppressFBWarnings(value = "SPRING_FILE_DISCLOSURE", justification = "Still comes from code.")
	private static ModelAndView toSuccessView(final String requestedWith, final Supplier<String> redirectSupplier) {
		return new ModelAndView(
				"XMLHttpRequest".equals(requestedWith) ? "success" : "redirect:/" + redirectSupplier.get());
	}

	private static ModelAndView toErrorView(final String message) {
		final ModelAndView modelAndView = new ModelAndView("error", HttpStatus.BAD_REQUEST);
		modelAndView.getModelMap().addAttribute("errorMessage", message);
		return modelAndView;
	}
}
