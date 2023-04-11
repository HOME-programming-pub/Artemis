package de.tum.in.www1.artemis.service;

import java.awt.image.BufferedImage;
import java.io.*;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import de.tum.in.www1.artemis.domain.lecture.AttachmentUnit;
import de.tum.in.www1.artemis.domain.lecture.Slide;
import de.tum.in.www1.artemis.repository.SlideRepository;
import de.tum.in.www1.artemis.web.rest.errors.InternalServerErrorException;

/**
 * Service Implementation for managing the split of AttachmentUnit into single slides and save them as PNG.
 */
@Service
public class SlideSplitterService {

    private final Logger log = LoggerFactory.getLogger(SlideSplitterService.class);

    private final FileService fileService;

    private final SlideRepository slideRepository;

    public SlideSplitterService(FileService fileService, SlideRepository slideRepository) {
        this.fileService = fileService;
        this.slideRepository = slideRepository;
    }

    /**
     * Splits an Attachment Unit file into single slides and saves them as PNG files asynchronously.
     *
     * @param attachmentUnit The attachment unit to which the slides belong.
     */
    @Async
    public void splitAttachmentUnitIntoSingleSlides(AttachmentUnit attachmentUnit) {
        splitAttachmentUnitIntoSingleSlides(null, attachmentUnit);
    }

    /**
     * Splits an Attachment Unit file into single slides and saves them as PNG files.
     *
     * @param attachmentUnit The attachment unit to which the slides belong.
     * @param document       The PDF document that is already loaded.
     */
    public void splitAttachmentUnitIntoSingleSlides(PDDocument document, AttachmentUnit attachmentUnit) {
        log.debug("Splitting Attachment Unit file {} into single slides", attachmentUnit.getAttachment().getName());
        try {
            String attachmentPath = fileService.actualPathForPublicPath(attachmentUnit.getAttachment().getLink());
            File file = new File(attachmentPath);
            if (document == null) {
                document = PDDocument.load(file);
            }
            String pdfFilename = file.getName();
            String fileNameWithOutExt = FilenameUtils.removeExtension(pdfFilename);
            int numPages = document.getNumberOfPages();
            PDFRenderer pdfRenderer = new PDFRenderer(document);

            for (int page = 0; page < numPages; ++page) {
                BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(page, 72, ImageType.RGB);
                byte[] imageInByte = bufferedImageToByteArray(bufferedImage, "png");
                MultipartFile slideFile = fileService.convertByteArrayToMultipart(fileNameWithOutExt + "_" + attachmentUnit.getId() + "_Slide_" + (page + 1), ".png", imageInByte);
                String filePath = fileService.handleSaveFile(slideFile, true, false);
                Slide slideEntity = new Slide();
                slideEntity.setSlideImagePath(filePath);
                slideEntity.setSlideNumber(page + 1);
                slideEntity.setAttachmentUnit(attachmentUnit);
                slideRepository.save(slideEntity);
            }

            document.close(); // make sure to close the document
        }
        catch (IOException e) {
            log.error("Error while splitting Attachment Unit into single slides", e);
            throw new InternalServerErrorException("Could not split Attachment Unit into single slides");
        }
    }

    /**
     * Converts BufferedImage to byte[]
     *
     * @param bufferedImage the image to convert
     * @param format        the format of the image (e.g. png)
     */
    private byte[] bufferedImageToByteArray(BufferedImage bufferedImage, String format) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImage, format, outputStream);
            return outputStream.toByteArray();
        }
    }
}