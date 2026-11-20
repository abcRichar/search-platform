package com.example.search.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.search.model.Post;

@Mapper
public interface PostMapper {

    @Select("select * from posts where id = #{id}")
    Post findById(Long id);

    @Select("select * from posts order by create_time desc")
    List<Post> findAll();

    @Select("select p.*, u.username as author from posts p " +
            "left join users u on p.user_id = u.id " +
            "order by p.create_time desc")
    List<java.util.Map<String, Object>> findAllWithAuthor();

    @Insert("insert into posts(title, content, tags, user_id, create_time, update_time) " +
            "values(#{title}, #{content}, #{tags}, #{userId}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Post post);

    @Update("update posts set title = #{title}, content = #{content}, tags = #{tags}, update_time = now() " +
            "where id = #{id}")
    int update(Post post);

    @Delete("delete from posts where id = #{id}")
    int delete(Long id);
}
