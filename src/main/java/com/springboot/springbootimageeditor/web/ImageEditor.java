package com.springboot.springbootimageeditor.web;

import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.PathParam;

@RestController
public class ImageEditor {

    private ImageProcessorController imageProcessorController = new ImageProcessorController();

    @RequestMapping("/hello")
    public String sayHello(@RequestParam(value = "name") String name) {
        return "Hello " + name + "!";
    }

    @RequestMapping(value = "/image", method = RequestMethod.POST)
    public JSONObject addImage(HttpServletRequest requestEntity) throws Exception {
        return imageProcessorController.setImage(requestEntity.getInputStream());
    }

    @RequestMapping(value = "/image/{id}", method = RequestMethod.DELETE)
    public ResponseEntity deleteImage(@PathVariable String id){
        if(imageProcessorController.imageMapContains(id)){
            imageProcessorController.deleteImage(id);
            return null;
        }else{
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ID not found");
        }
    }

    @RequestMapping(value = "/image/{id}/size", method = RequestMethod.GET)
    public JSONObject getSize(@PathVariable String id){
        if(imageProcessorController.imageMapContains(id)){
            return imageProcessorController.getSize(id);
        }else{
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ID not found");
        }
    }

    @RequestMapping(value = "/image/{id}/scale/{percent}", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] getRescaledImage(@PathVariable String id, @PathVariable double percent)  throws Exception {
        if(imageProcessorController.imageMapContains(id)){
            return imageProcessorController.getScaledImage(id, percent);
        }else{
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ID not found");
        }
    }

    @RequestMapping(value = "/image/{id}/histogram", method = RequestMethod.GET)
    public JSONObject getHistogram(@PathVariable String id) {
        if(imageProcessorController.imageMapContains(id)){
            return imageProcessorController.getHistogram(id);
        }else{
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ID not found");
        }
    }


    @RequestMapping(value = "/image/{id}/crop/{start}/{stop}/{width}/{height}", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] getRescaledImage(@PathVariable String id, @PathVariable int start, @PathVariable int stop, @PathVariable int width, @PathVariable int height)  throws Exception {
        if(imageProcessorController.imageMapContains(id)){
            try {
                return imageProcessorController.getCroppedImage(id, start, stop, width, height);
            }catch(ResponseStatusException e){
                throw new ResponseStatusException(e.getStatus(), e.getMessage());
            }
        }
        else{
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "entity not found");
        }
    }


    @RequestMapping(value = "/image/{id}/greyscale", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] getRescaledImage(@PathVariable String id)  throws Exception {
        if(imageProcessorController.imageMapContains(id)){
            return imageProcessorController.getGreyScale(id);
        }
        else{
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
        }
    }





}

