package com.scheible.rocastyle.projectplanner.domain;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

/**
 *
 * @author sj
 */
@Service
public class ProjectService {

	public static class Task {

		private final long id;
		private final String name;

		private String url;

		private Task(final long id, final String name) {
			this.id = id;
			this.name = name;
		}

		private void setUrl(final String url) {
			this.url = url;
		}

		public long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getUrl() {
			return url;
		}
	}

	public static class Milestone {

		private final long id;
		private final String name;

		private final List<Task> tasks = new ArrayList<>();

		private Milestone(final long id, final String name) {
			this.id = id;
			this.name = name;
		}

		public long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<Task> getTasks() {
			return unmodifiableList(tasks);
		}
	}

	public static class Project {

		public enum Order {
			BEFORE, AFTER
		}

		private final long id;
		private String name;

		private long revision;

		private final List<Milestone> milestones = new ArrayList<>();

		private Project(final long id, final String name) {
			this.id = id;
			this.name = name;
		}

		/**
		 * NOTE Not implemented... would require cloning of data and a real save method.
		 */
		public void setRevision(final long revision) {
			this.revision = revision;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public void addMilestone(final String name, final Optional<Entry<Order, Long>> anchor) {
			final boolean alreadyThere = milestones.stream().filter(p -> p.name.equals(name)).findFirst().isPresent();
			if (!alreadyThere) {
				final int index = anchor.map(a -> {
					int anchorIndex = -1;
					for (int i = 0; i < milestones.size(); i++) {
						if (milestones.get(i).id == a.getValue()) {
							anchorIndex = i;
							break;
						}
					}
					if (anchorIndex == -1) {
						return null;
					}

					return anchorIndex + (a.getKey() == Order.AFTER ? 1 : 0);
				}).orElseGet(() -> milestones.size());

				milestones.add(index, new Milestone(nextSequenceValue(), name));
			}
		}

		public void deleteMilestone(final long milestoneId) {
			for (int i = 0; i < milestones.size(); i++) {
				if (milestones.get(i).id == milestoneId) {
					milestones.remove(i);
					return;
				}
			}
		}

		public void addTask(final long milestoneId, final String name, final Optional<Entry<Order, Long>> anchor) {
			milestones.stream().filter(p -> p.id == milestoneId).forEach(p -> {
				final boolean alreadyThere = p.tasks.stream().filter(t -> t.name.equals(name)).findFirst().isPresent();
				if (!alreadyThere) {
					final int index = anchor.map(a -> {
						int anchorIndex = -1;
						for (int i = 0; i < p.tasks.size(); i++) {
							if (p.tasks.get(i).id == a.getValue()) {
								anchorIndex = i;
								break;
							}
						}
						if (anchorIndex == -1) {
							return null;
						}

						return anchorIndex + (a.getKey() == Order.AFTER ? 1 : 0);
					}).orElseGet(() -> p.tasks.size());

					p.tasks.add(index, new Task(nextSequenceValue(), name));
				}
			});
		}

		public void deleteTask(final long milestoneId, final long taskId) {
			milestones.stream().filter(p -> p.id == milestoneId).forEach(p -> {
				for (int i = 0; i < p.tasks.size(); i++) {
					if (p.tasks.get(i).id == taskId) {
						p.tasks.remove(i);
						return;
					}
				}
			});
		}

		public void changeTaskUrl(final long milestoneId, final long taskId, final String url) {
			milestones.stream().filter(p -> p.id == milestoneId).forEach(p -> {
				p.tasks.stream().filter(t -> t.id == taskId).findFirst().ifPresent(t -> {
					t.setUrl(url);
				});
			});
		}

		public long getId() {
			return id;
		}

		/**
		 * NOTE Not implemented... would require cloning of data and a real save method.
		 */
		public long getRevision() {
			return revision;
		}

		public String getName() {
			return name;
		}

		public List<Milestone> getMilestones() {
			return unmodifiableList(milestones);
		}
	}

	private static final AtomicLong SEQUENCE = new AtomicLong(0);

	private final Map<Long, Project> projects = new HashMap<>();

	public Project findOrCreateProject(final long id) {
		return Optional.ofNullable(projects.get(id)).orElseGet(() -> {
			final Project newProject = new Project(id, "Unnamed");
			projects.put(newProject.getId(), newProject);
			return newProject;
		});
	}

	private static long nextSequenceValue() {
		return SEQUENCE.getAndIncrement();
	}
}
