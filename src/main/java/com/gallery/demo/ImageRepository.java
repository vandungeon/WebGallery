package com.gallery.demo;

import org.springframework.data.jpa.repository.JpaRepository;


public interface ImageRepository extends JpaRepository<DataImage, Long> {
}
