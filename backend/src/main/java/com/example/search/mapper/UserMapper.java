package com.example.search.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.search.model.User;

@Mapper
public interface UserMapper {

    @Select("select * from users where id = #{id}")
    User findById(Long id);

    @Select("select * from users where username = #{username}")
    User findByUsername(String username);

    @Select("select * from users")
    List<User> findAll();

    @Insert("insert into users(username, password, role, avatar_url, create_time, update_time) " +
            "values(#{username}, #{password}, #{role}, #{avatarUrl}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Delete("delete from users where id = #{id}")
    int delete(Long id);
}
