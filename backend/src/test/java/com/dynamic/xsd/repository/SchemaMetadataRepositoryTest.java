package com.dynamic.xsd.repository;

import com.dynamic.xsd.domain.entity.SchemaMetadata;
import com.dynamic.xsd.domain.enums.SchemaStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JPA tests for SchemaMetadataRepository
 */
@DataJpaTest
class SchemaMetadataRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SchemaMetadataRepository repository;

    @Test
    void testSave_NewSchema_Success() {
        // Arrange
        SchemaMetadata schema = createTestSchema("test-service");

        // Act
        SchemaMetadata saved = repository.save(schema);

        // Assert
        assertNotNull(saved.getId());
        assertEquals("test-service", saved.getServiceName());
        assertEquals("1.0", saved.getVersion());
    }

    @Test
    void testFindByServiceName_ExistingSchema_ReturnsSchema() {
        // Arrange
        SchemaMetadata schema = createTestSchema("find-test");
        entityManager.persist(schema);
        entityManager.flush();

        // Act
        Optional<SchemaMetadata> found = repository.findByServiceName("find-test");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("find-test", found.get().getServiceName());
    }

    @Test
    void testFindByServiceName_NonExistent_ReturnsEmpty() {
        // Act
        Optional<SchemaMetadata> found = repository.findByServiceName("non-existent");

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void testFindByStatus_MultipleSchemas_ReturnsFiltered() {
        // Arrange
        SchemaMetadata active1 = createTestSchema("active-1");
        active1.setStatus(SchemaStatus.ACTIVE);

        SchemaMetadata active2 = createTestSchema("active-2");
        active2.setStatus(SchemaStatus.ACTIVE);

        SchemaMetadata failed = createTestSchema("failed-1");
        failed.setStatus(SchemaStatus.FAILED);

        entityManager.persist(active1);
        entityManager.persist(active2);
        entityManager.persist(failed);
        entityManager.flush();

        // Act
        List<SchemaMetadata> activeSchemas = repository.findByStatus(SchemaStatus.ACTIVE);

        // Assert
        assertEquals(2, activeSchemas.size());
        assertTrue(activeSchemas.stream().allMatch(s -> s.getStatus() == SchemaStatus.ACTIVE));
    }

    @Test
    void testFindAll_WithPagination_ReturnsPage() {
        // Arrange
        for (int i = 0; i < 25; i++) {
            SchemaMetadata schema = createTestSchema("schema-" + i);
            entityManager.persist(schema);
        }
        entityManager.flush();

        // Act
        Page<SchemaMetadata> page = repository.findAll(PageRequest.of(0, 10));

        // Assert
        assertEquals(10, page.getContent().size());
        assertTrue(page.getTotalElements() >= 25);
        assertTrue(page.getTotalPages() >= 3);
    }

    @Test
    void testFindAll_WithSorting_ReturnsSorted() {
        // Arrange
        SchemaMetadata schema1 = createTestSchema("zzz");
        SchemaMetadata schema2 = createTestSchema("aaa");
        SchemaMetadata schema3 = createTestSchema("mmm");

        entityManager.persist(schema1);
        entityManager.persist(schema2);
        entityManager.persist(schema3);
        entityManager.flush();

        // Act
        List<SchemaMetadata> sorted = repository.findAll(Sort.by(Sort.Direction.ASC, "serviceName"));

        // Assert
        assertTrue(sorted.size() >= 3);
        int aaaIndex = -1, mmmIndex = -1, zzzIndex = -1;
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getServiceName().equals("aaa")) aaaIndex = i;
            if (sorted.get(i).getServiceName().equals("mmm")) mmmIndex = i;
            if (sorted.get(i).getServiceName().equals("zzz")) zzzIndex = i;
        }
        assertTrue(aaaIndex < mmmIndex && mmmIndex < zzzIndex);
    }

    @Test
    void testDelete_ExistingSchema_Success() {
        // Arrange
        SchemaMetadata schema = createTestSchema("delete-test");
        entityManager.persist(schema);
        entityManager.flush();
        String id = schema.getId();

        // Act
        repository.deleteById(id);

        // Assert
        assertFalse(repository.findById(id).isPresent());
    }

    @Test
    void testUpdate_ExistingSchema_Success() {
        // Arrange
        SchemaMetadata schema = createTestSchema("update-test");
        entityManager.persist(schema);
        entityManager.flush();

        // Act
        schema.setStatus(SchemaStatus.ACTIVE);
        schema.setDescription("Updated description");
        SchemaMetadata updated = repository.save(schema);

        // Assert
        assertEquals(SchemaStatus.ACTIVE, updated.getStatus());
        assertEquals("Updated description", updated.getDescription());
    }

    @Test
    void testCount_ReturnsCorrectCount() {
        // Arrange
        for (int i = 0; i < 5; i++) {
            entityManager.persist(createTestSchema("count-test-" + i));
        }
        entityManager.flush();

        // Act
        long count = repository.count();

        // Assert
        assertTrue(count >= 5);
    }

    @Test
    void testExistsByServiceName_ExistingSchema_ReturnsTrue() {
        // Arrange
        SchemaMetadata schema = createTestSchema("exists-test");
        entityManager.persist(schema);
        entityManager.flush();

        // Act
        boolean exists = repository.existsByServiceName("exists-test");

        // Assert
        assertTrue(exists);
    }

    @Test
    void testExistsByServiceName_NonExistent_ReturnsFalse() {
        // Act
        boolean exists = repository.existsByServiceName("non-existent");

        // Assert
        assertFalse(exists);
    }

    // Helper method
    private SchemaMetadata createTestSchema(String serviceName) {
        SchemaMetadata schema = new SchemaMetadata();
        schema.setServiceName(serviceName);
        schema.setVersion("1.0");
        schema.setTargetNamespace("http://example.com/" + serviceName);
        schema.setStatus(SchemaStatus.UPLOADED);
        schema.setUploadedBy("test-user");
        schema.setUploadedAt(LocalDateTime.now());
        schema.setXsdFilePath("/tmp/" + serviceName + ".xsd");
        schema.setPackageName("com.example." + serviceName.replace("-", "."));
        return schema;
    }
}
