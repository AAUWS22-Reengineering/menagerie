package menagerie.model.menagerie.importer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class ImportJobQueue {

  private static final Logger LOGGER = Logger.getLogger(ImportJobQueue.class.getName());

  private final Queue<ImportJob> queue = new ConcurrentLinkedQueue<>();
  private final Consumer<ImportJob> runJob;

  public ImportJobQueue(Consumer<ImportJob> runJob) {
    this.runJob = runJob;
  }

  void startNextImportJob() {
    LOGGER.info(() -> "Import queue size: " + queue.size());
    ImportJob job = queue.remove();

    if (job.getUrl() != null) {
      LOGGER.info(() -> "Starting web import: " + job.getUrl());
    } else {
      LOGGER.info(() -> "Starting local import: " + job.getFile());
    }
    runJob.accept(job);
    LOGGER.info(() -> "Finished import: " + job.getItem().getId());
  }

  boolean isEmpty() {
    return queue.isEmpty();
  }

  void add(ImportJob job) {
    queue.add(job);
  }

  void remove(ImportJob job) {
    queue.remove(job);
  }
}
